package info.kgeorgiy.ja.churakova.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class WebCrawler implements Crawler {

    private final Downloader downloader;
    private final ExecutorService downloaders;
    private final ExecutorService extractors;
    private final ConcurrentMap<String, HostHandler> hosts;
    private final int perHost;

    /**
     * Constructs class with given {@link Downloader}, amount of downloaders, extractors, defined host load
     *
     * @param downloader  {@link CachingDownloader} downloads pages and extracts links from them
     * @param downloaders limit for onetime downloaded pages
     * @param extractors  maximal amount of pages from which onetime links extracts
     * @param perHost     limit for onetime downloaded pages from single host
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
        hosts = new ConcurrentHashMap<>();
        this.perHost = perHost;
    }

    /**
     * Downloads website up to specified depth.
     *
     * @param url   start <a href="http://tools.ietf.org/html/rfc3986">URL</a>.
     * @param depth download depth.
     * @return {@link Result} containing downloaded urls and errors mapped for urls on which download errors occur
     */
    @Override
    public Result download(String url, int depth) {
        ConcurrentMap<String, IOException> errors = new ConcurrentHashMap<>();
        Set<String> result = new CopyOnWriteArraySet<>();
        Set<String> visitedLinks = new CopyOnWriteArraySet<>();
        visitedLinks.add(url);
        Phaser phaser = new Phaser(1);
        bfs(errors, result, visitedLinks, phaser, url, depth - 1);
        phaser.arriveAndAwaitAdvance();
        return new Result(new ArrayList<>(result), errors);
    }

    /**
     * Closes this web-crawler, relinquishing any allocated resources.
     */
    @Override
    public void close() {
        downloaders.shutdown();
        extractors.shutdown();
    }

    private void bfs(ConcurrentMap<String, IOException> exceptions, Set<String> result, Set<String> visited, Phaser phaser,
                     String url, int depth) {
        BlockingQueue<Link> linkQueue = new ArrayBlockingQueue<>(10000);
        addLinkOrException(linkQueue, exceptions, url, depth);
        while (!linkQueue.isEmpty()) {
            Link currentLink = linkQueue.poll();
            phaser.register();
            currentLink.addNewTask(
                    () -> downloadersTask(linkQueue, currentLink, result, visited, exceptions, phaser));

            if (linkQueue.isEmpty()) {
                phaser.arriveAndAwaitAdvance();
            }
        }

    }

    private void downloadersTask(BlockingQueue<Link> linkQueue, Link curLink, Set<String> result, Set<String> visited,
                                 ConcurrentMap<String, IOException> exceptions, Phaser phaser) {
        try {
            Document doc = downloader.download(curLink.getLink());
            result.add(curLink.getLink());
            if (curLink.getDepth() > 0) {
                phaser.register();
                extractors.submit(() -> extractorsTask(linkQueue, doc, curLink, visited, exceptions, phaser));
            }
        } catch (IOException e) {
            exceptions.put(curLink.getLink(), e);
        } finally {
            phaser.arriveAndDeregister();
            curLink.produceNextTask();
        }
    }

    private void extractorsTask(BlockingQueue<Link> linkQueue, Document doc, Link curLink, Set<String> visited,
                                ConcurrentMap<String, IOException> exceptions,
                                Phaser phaser) {
        try (Stream<String> links = doc.extractLinks().stream()) {
            links.filter(visited::add).forEach(
                    link -> addLinkOrException(linkQueue, exceptions, link, curLink.getDepth() - 1));

        } catch (IOException l) {
            exceptions.put(curLink.getLink(), l);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    private void addLinkOrException(BlockingQueue<Link> linkQueue, ConcurrentMap<String, IOException> exceptions,
                                    String link, int depth) {
        try {
            while (!linkQueue.offer(
                    new Link(link, new AtomicInteger(depth), hosts.computeIfAbsent(URLUtils.getHost(link), host -> new HostHandler())
                    ))) ;
        } catch (MalformedURLException mUrl) {
            exceptions.put(link, mUrl);
        }
    }

    private record Link(String link, AtomicInteger depth, HostHandler handler) {
        // it seems like atomic depth should not affect
        public String getLink() {
            return link;
        }

        public int getDepth() {
            return depth.get();
        }

        public void addNewTask(Runnable task) {
            handler.submitOrDelay(task);
        }

        public void produceNextTask() {
            handler.nextOrRelieve();
        }
    }

    private class HostHandler {
        private final Queue<Runnable> queue;
        AtomicInteger onHost;

        public HostHandler() {
            // in host queue can be added no more than in queue in bfs
            queue = new ConcurrentLinkedQueue<>();
            onHost = new AtomicInteger(0);
        }

        public void submitOrDelay(Runnable task) {
            if (onHost.get() >= perHost) {
                queue.add(task);
            } else {
                downloaders.submit(task);
                onHost.incrementAndGet();
            }
        }

        public void nextOrRelieve() {
            if (!queue.isEmpty()) {
                downloaders.submit(queue.poll());
            } else {
                onHost.decrementAndGet();
            }
        }
    }

    private static int[] verifyArgs(String[] args) {
        if (args == null || args.length != 5) {
            throw new IllegalArgumentException("Invalid arguments. Expected from 1 to 5 arguments");
        }
        int[] arguments = new int[4];
        try {
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = Integer.parseInt(args[i + 1]);
                if (arguments[i] <= 0) {
                    throw new IllegalArgumentException("Only positive values of arguments expected.");
                }
            }
        } catch (NumberFormatException numb) {
            throw new IllegalArgumentException("Can not parse invalid arguments" + numb.getMessage());
        }
        return arguments;
    }

    /**
     * An entry point. Initialise class with {@link CachingDownloader}, given amount of downloaders and executors, defined host load
     *
     * @param args url [depth [downloads [extractors [perHost]]]]
     */
    public static void main(String[] args) {
        int[] arguments = verifyArgs(args);
        String url = args[0];
        try (WebCrawler webCrawler = new WebCrawler(new CachingDownloader(1), arguments[0], arguments[1], arguments[2])) {
            webCrawler.download(url, arguments[3]);
        } catch (IOException e) {
            System.err.printf("Error occur during creating CachingDownloader%n%s%n", e.getMessage());
        }
    }
}


