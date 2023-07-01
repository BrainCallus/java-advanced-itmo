package info.kgeorgiy.ja.churakova.arrayset;

import java.util.*;

// :NOTE: На момент проверки не проходит даже easy тесты (Г.Н., 10.03.22, 22:23 msk)

public class ArraySet<E extends Comparable<E>> extends AbstractSet<E> implements NavigableSet<E> {
    private static final String UNSUPPORTED = "This set is immutable, pls do not try to modify it!";

    private final DescendingList<E> setElements;
    private final Comparator<? super E> comparator;

    public ArraySet(Collection<? extends E> set, Comparator<? super E> comparator) {
        this.comparator = comparator;
        Set<E> temp = new TreeSet<>(comparator);
        temp.addAll(set);
        this.setElements = new DescendingList<>(temp.stream().toList(), false);
    }

    public ArraySet(Comparator<? super E> comparator) {
        this.comparator = comparator;
        this.setElements = new DescendingList<>(Collections.emptyList(), false);
    }

    public ArraySet() {
        this.comparator = null;
        this.setElements = new DescendingList<>(Collections.emptyList(), false);
    }

    public ArraySet(Collection<? extends E> set) {
        this.comparator = null;
        this.setElements = new DescendingList<>(new ArrayList<>(new TreeSet<>(set)), false);
    }

    private ArraySet(DescendingList<E> set, Comparator<? super E> comparator) {
        this.comparator = comparator;
        this.setElements = set;
    }

    // :NOTE: Создание ArraySet от SortedSet должно сохранять порядок элементов из SortedSet
    // :NOTE: не исправлено

    private boolean verifyIndex(int index) {
        return index >= 0 && index < size();
    }

    private int binSearchInd(E element, boolean isLower, boolean included) {
        int ind = Collections.binarySearch(setElements, element, comparator);
        if (ind < 0) {
            ind = -(ind + 1);
            return isLower ? --ind : ind;
        }
        if (isLower ^ included) {
            ind -= isLower ? 1 : 0;
        } else {
            ind += isLower & included ? 0 : 1;
        }
        return ind;
    }


    @Override
    public E lower(E element) {
        int ind = binSearchInd(element, true, false);
        return verifyIndex(ind) ? setElements.get(ind) : null;
    }

    @Override
    public E higher(E element) {
        int ind = binSearchInd(element, false, false);
        return verifyIndex(ind) ? setElements.get(ind) : null;
    }

    @Override
    public E floor(E element) {
        int ind = binSearchInd(element, true, true);
        return verifyIndex(ind) ? setElements.get(ind) : null;
    }

    @Override
    public E ceiling(E element) {
        int ind = binSearchInd(element, false, true);
        return verifyIndex(ind) ? setElements.get(ind) : null;
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableList(setElements).iterator();
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new Iterator<>() {
            final ListIterator<E> iterator =
                    Collections.unmodifiableList(setElements).listIterator(size());

            @Override
            public boolean hasNext() {
                return iterator.hasPrevious();
            }

            @Override
            public E next() {
                return iterator.previous();
            }
        };
    }

    // :NOTE: descendingSet() не должен менять исходный set. В вашем случае -- меняет порядок элементов
    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(new DescendingList<>(setElements.listElements.stream().toList(), !setElements.isDesc), Collections.reverseOrder(comparator));
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        if (isEmpty()) {
            return new ArraySet<E>(comparator);
        }
        return getSegment(0, binSearchInd(toElement, true, inclusive));
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        if (isEmpty()) {
            return new ArraySet<E>(comparator);
        }
        return getSegment(binSearchInd(fromElement, false, inclusive), size() - 1);
    }

    private void verifyArgs(E first, E second) {
        if (comparator != null && comparator.compare(first, second) > 0
                || comparator == null && first.compareTo(second) > 0) {
            throw new IllegalArgumentException("First argument > second");
        }
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromIncl, E toElement, boolean toIncl) {
        verifyArgs(fromElement, toElement);
        int fromInd = binSearchInd(fromElement, false, fromIncl);
        int toInd = binSearchInd(toElement, true, toIncl);
        return getSegment(fromInd, toInd);
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    private NavigableSet<E> getSegment(int fromInd, int toInd) {
        if (!(verifyIndex(fromInd) && verifyIndex(toInd)) || fromInd > toInd) {
            return new ArraySet<E>(comparator);
        }

        return new ArraySet<>(new DescendingList<>(
                setElements.subList(fromInd, toInd + 1), setElements.isDesc),
                comparator);
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public E first() {
        if (isEmpty()) {
            throw new NoSuchElementException("Set is empty!");
        }
        return setElements.get(0);
    }

    @Override
    public E last() {
        if (isEmpty()) {
            throw new NoSuchElementException("Set is empty!");
        }
        return setElements.get(size() - 1);
    }

    @Override
    public int size() {
        return setElements.size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object element) {
        return Collections.binarySearch(setElements, (E) element, comparator) >= 0;
    }


// UNSUPPORTED METHODS

    @Override
    public E pollFirst() { //  IMMUTABLE -> MY_EXCEPTION
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    public E pollLast() { //  IMMUTABLE -> MY_EXCEPTION
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    // :NOTE: следующие 6 методов не нужны


    private static class DescendingList<E> extends AbstractList<E> implements RandomAccess {
        private final List<E> listElements;
        private final boolean isDesc;

        public DescendingList(List<E> listElements, boolean isDesc) {
            this.listElements = listElements;
            this.isDesc = isDesc;
        }

        @Override
        public int size() {
            return listElements.size();
        }

        @Override
        public E get(int index) {
            return isDesc ? listElements.get(size() - index - 1) : listElements.get(index);
        }
    }

}
