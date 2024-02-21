package info.kgeorgiy.ja.churakova.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.List;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> threads;
    private final Deque<JobSlice<?>> queue;

    /**
     * Constructs class with given amount of threads
     *
     * @param threads_cnt amount of threads that will do parallel work
     */
    public ParallelMapperImpl(int threads_cnt) {
        this.threads = new ArrayList<>();
        this.queue = new ArrayDeque<>();
        for (int i = 0; i < threads_cnt; i++) {
            threads.add(getStarted());
        }
    }

    /**
     * Maps function {@code f} over specified {@code args}
     *
     * @param f    mapper function
     * @param args arguments to map
     * @param <T>  type of elements in list {@code args}
     * @param <R>  type of elements got by mapping given list
     * @return list of elements got by mapping given list of elements {@code args}
     * @throws InterruptedException if during executing some threads were interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        final CompleteChecker<R> checker = new CompleteChecker<>(args.size());
        addJobSlices(getJob(f, args, checker));
        synchronized (checker) {
            while (!checker.isCompleted()) {
                checker.wait();
            }
        }
        return checker.getResults(); //or throw exception
    }

    /**
     * Stops all threads. All unfinished mappings are left in undefined state.
     */
    @Override
    public void close() {
        threads.forEach(Thread::interrupt);
        threads.forEach(thread -> {
            while (thread.isAlive()) {
                try {
                    thread.join();
                } catch (InterruptedException inter) {
                    // ignore
                }
            }
        });
    }

    private <E, T> List<JobSlice<?>> getJob(Function<? super E, ? extends T> f, List<E> args, CompleteChecker<T> checker) {
        List<JobSlice<?>> job = new ArrayList<>();
        int i = 0;
        for (E arg : args) {
            final int index = i++;
            job.add(new JobSlice<>(() -> checker.results.set(index, f.apply(arg)), checker));
        }
        return job;
    }

    private void addJobSlices(List<JobSlice<?>> slices) {
        synchronized (queue) {
            queue.addAll(slices);
            queue.notifyAll();
        }
    }

    private Thread getStarted() {
        Thread thread = new Thread(() -> {
            try {
                while (!Thread.interrupted()) {
                    launchJob(extractSubtask());
                }
            } catch (InterruptedException ignored) {
            }
        });
        thread.start();
        return thread;
    }

    private void launchJob(JobSlice<?> slice) {
        try {
            slice.subtask.run();
        } catch (RuntimeException e) {
            synchronized (slice.checker) {
                slice.checker.errors.addSuppressed(e);
            }
        }
        synchronized (slice.checker) {
            slice.checker.completed++;
            if (slice.checker.isCompleted()) {
                slice.checker.notify();
            }
        }
    }

    private JobSlice<?> extractSubtask() throws InterruptedException {
        synchronized (queue) {
            while (queue.isEmpty()) {
                queue.wait();
            }
            return queue.poll();
        }
    }

    private static class CompleteChecker<R> {
        private int completed;
        private final int toComplete;
        private final List<R> results;
        private final RuntimeException errors;

        public CompleteChecker(int toComplete) {
            this.toComplete = toComplete;
            this.results = new ArrayList<>(Collections.nCopies(toComplete, null));
            errors = new RuntimeException("Unable to reckoner results because of occur errors");
        }

        public boolean isCompleted() {
            return completed == toComplete;
        }

        public List<R> getResults() {
            if (errors.getSuppressed().length != 0) {
                throw errors;
            }
            return results;
        }
    }

    private static class JobSlice<R> {
        final Runnable subtask;
        final CompleteChecker<R> checker;

        public JobSlice(Runnable subtask, CompleteChecker<R> counter) {
            this.subtask = subtask;
            this.checker = counter;
        }
    }
}
