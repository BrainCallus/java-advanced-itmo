package info.kgeorgiy.ja.churakova.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements AdvancedIP {
    private final ParallelMapper parallelMapper;


    /**
     * Constructs class with given {@link ParallelMapper}
     *
     * @param parallelMapper {@link ParallelMapper} maintain parallel execution
     */
    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    /**
     * Default constructor
     */
    public IterativeParallelism() {
        this.parallelMapper = null;
    }


    /**
     * Transforms given list of elements into string
     * <p>
     * For each object in list call to {@link Object#toString} and concatenate these string values
     *
     * @param threads number of concurrent threads.
     * @param values  values to join.
     * @return string consists of string values of elements from given list
     * @throws InterruptedException if during executing some threads were interrupted
     */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return execute(threads, values, stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    /**
     * Filters elements from given list by predicate
     *
     * @param threads   number of concurrent threads.
     * @param values    values to filter.
     * @param predicate filter predicate.
     * @param <T>       type that extends elements in list values
     * @return list of filtered elements
     * @throws InterruptedException if during executing some threads were interrupted
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return execute(threads, values, stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    /**
     * Using parameter function get from given list of elements list of values that have type that is function return type
     *
     * @param threads number of concurrent threads.
     * @param values  values to map.
     * @param f       mapper function.
     * @param <T>     - type of elements in list values
     * @param <U>     - type of elements in returnable list
     * @return list of elements mapped by function from given list of values
     * @throws InterruptedException if during executing some threads were interrupted
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return execute(threads, values, stream -> stream.map(f).collect(Collectors.toList()), stream -> stream
                .flatMap(Collection::stream).collect(Collectors.toList()));
    }

    /**
     * Minimum from given values
     * <p>
     * If list of values hasn't got maximum the {@link NoSuchElementException} will thrown
     *
     * @param threads    number of concurrent threads.
     * @param values     values to get minimum of.
     * @param comparator value comparator.
     * @param <T>        type of elements in list values
     * @return minimum from given values
     * @throws InterruptedException if during executing some threads were interrupted
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return execute(threads, values, stream -> stream.max(comparator).orElse(null),
                stream -> stream.max(comparator).orElseThrow());
    }

    /**
     * Maximum from given values
     * <p>
     * If list of values hasn't got maximum the {@link NoSuchElementException} will thrown
     *
     * @param threads    number of concurrent threads.
     * @param values     values to get minimum of.
     * @param comparator value comparator.
     * @param <T>        type of elements in list values
     * @return maximum from given values
     * @throws InterruptedException if during executing some threads were interrupted
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    /**
     * Check whether at least any one of values satisfies the predicate
     *
     * @param threads   number of concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       type of elements in list values
     * @return whether at least any one of values satisfies the predicate
     * @throws InterruptedException if during executing some threads were interrupted
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return execute(threads, values, stream -> stream.anyMatch(predicate),
                stream -> stream.anyMatch(res -> res));
    }

    /**
     * Counting elements that satisfies to predicate
     *
     * @param threads   number of concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       type of elements in list values
     * @return amount of elements that satisfies to predicate
     * @throws InterruptedException if during executing some threads were interrupted
     */
    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return (int) ((long) execute(threads, values, stream -> stream.filter(predicate).count(),
                stream -> stream.reduce(this::sumAccum).orElse(0L)));

    }

    private Long sumAccum(Long a, Long b) {
        return a + b;
    }

    /**
     * Check whether all values satisfy the predicate
     *
     * @param threads   number of concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       type of parameter values
     * @return whether all values satisfy the predicate
     * @throws InterruptedException if during executing some threads were interrupted
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !any(threads, values, predicate.negate());
    }

    /**
     * Reduce values by provided monoid
     * <p>
     * if no values specified returns {@link Monoid#getIdentity() identity}
     *
     * @param threads number of concurrent threads.
     * @param values  values to reduce.
     * @param monoid  monoid to use.
     * @param <T>     type of elements in list values
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if no values specified
     * @throws InterruptedException if during executing some threads were interrupted
     */
    @Override
    public <T> T reduce(int threads, List<T> values, Monoid<T> monoid) throws InterruptedException {
        return values.stream().reduce(monoid.getOperator()).orElse(monoid.getIdentity());
    }

    /**
     * Maps values by mapping {@link Function} and reduce them by provided {@link Monoid}
     * <p>
     * if no values specified returns {@link Monoid#getIdentity() identity}
     *
     * @param threads number of concurrent threads.
     * @param values  values to reduce.
     * @param lift    mapping function.
     * @param monoid  monoid to use.
     * @param <T>     type of elements in list values
     * @param <R>     type of returnable element
     * @return element reduced by {@link Monoid} from list of elements that got by mapping given list values by {@link Function} lift
     * @throws InterruptedException if during executing some threads were interrupted
     */
    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, Monoid<R> monoid) throws InterruptedException {
        return reduce(threads, map(threads, values, lift), monoid);
    }


    private <E, T> E execute(int threads_cnt, List<T> elements, Function<Stream<T>, E> reckoner,
                             Function<Stream<E>, E> reducer) throws InterruptedException {
        checkArgs(threads_cnt);
        int amount = Math.min(threads_cnt, elements.size());
        if (parallelMapper == null) {
            return reducer.apply(executeWithoutMapper(getBlocks(elements, amount), reckoner));
        }
        return reducer.apply(parallelMapper.map(reckoner, getBlocks(elements, amount)).stream());

    }

    private <E, T> Stream<E> executeWithoutMapper(List<Stream<T>> blocks, Function<Stream<T>, E> func) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        List<E> results = new ArrayList<>(Collections.nCopies(blocks.size(), null));
        int ind = 0;
        for (Stream<T> block : blocks) {
            threads.add(getStarted(block, results, ind++, func));
        }
        for (Thread thread : threads) {
            thread.join();
        }
        return results.stream();
    }

    private <E, T> Thread getStarted(Stream<T> stream, List<E> results, int index, Function<Stream<T>, E> func) {
        Thread thread = new Thread(() -> results.set(index, func.apply(stream)));
        thread.start();
        return thread;
    }

    private <E> List<Stream<E>> getBlocks(List<E> elements, int amount) {
        List<Stream<E>> blocks = new ArrayList<>();
        if (amount == 0) {
            return blocks;
        }
        int size = elements.size() / amount;
        int ost = elements.size() % amount;
        int start = 0;
        for (int i = 0; i < amount; i++) {
            int cap = size + (ost-- > 0 ? 1 : 0);
            blocks.add(elements.subList(start, start + cap).stream());
            start += cap;
        }
        return blocks;
    }

    private void checkArgs(int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("Threads amount must be >=1");
        }
    }
}
