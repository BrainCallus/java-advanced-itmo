package info.kgeorgiy.ja.churakova.student;

import info.kgeorgiy.java.advanced.student.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedQuery {

    // ADVANCED_QUERY METHODS
    @Override
    public String getMostPopularName(Collection<Student> students) {

        return getLargest(students, Student::getFirstName,
                Collectors.mapping(Student::getGroup, Collectors.toSet()),
                Comparator.comparing((Map.Entry<String, Set<GroupName>> s) -> s.getValue().size()).reversed().
                        thenComparing(Map.Entry::getKey).reversed(), "");
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] ids) {

        return getByIds(students, ids, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] ids) {
        return getByIds(students, ids, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(Collection<Student> students, int[] ids) {
        return getByIds(students, ids, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] ids) {
        return getByIds(students, ids, getStudentFullName);
    }

    // GROUP_QUERY METHODS
    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return groupsBy(students, COMPARATOR_BY_NAME);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return groupsBy(students, Student::compareTo);
    }

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return getLargestGroup(students, List::size, GroupName::compareTo);
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroup(students, group -> getDistinctFirstNames(group).size(),
                Collections.reverseOrder(GroupName::compareTo));
    }

    // STUDENT_QUERY METHODS
    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getAttributes(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getAttributes(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return getAttributes(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getAttributes(students, getStudentFullName);
    }

    private final Function<Student, String> getStudentFullName = (student) ->
            student.getFirstName().concat(" ").concat(student.getLastName());


    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return students.stream().map(Student::getFirstName).sorted(String::compareTo).
                collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream().max(Student::compareTo).map(Student::getFirstName).orElse("");
    }


    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return getSortedStudents(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return getSortedStudents(students, COMPARATOR_BY_NAME);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsBy(students, Student::getFirstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsBy(students, Student::getLastName, name);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return findStudentsBy(students, Student::getGroup, group);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return findStudents(students, Student::getGroup, group).collect(Collectors.toMap(
                Student::getLastName, Student::getFirstName,
                BinaryOperator.minBy(String::compareTo)));
    }

    static private final Comparator<Student> COMPARATOR_BY_NAME = Comparator.comparing(Student::getLastName).
            thenComparing(Student::getFirstName).reversed().thenComparing(Student::compareTo);

    private final BiFunction<Collection<Student>, Comparator<Student>, Stream<Student>>
            sortedStudentStream = (students, comparator) -> students.stream().sorted(comparator);

    private List<Student> getSortedStudents(Collection<Student> students, Comparator<Student> comparator) {
        return sortedStudentStream.apply(students, comparator).collect(Collectors.toList());
    }

    private <T> List<Student> findStudentsBy(Collection<Student> students,
                                             Function<Student, T> getter, T value) {
        return findStudents(students, getter, value).sorted(COMPARATOR_BY_NAME).collect(Collectors.toList());
    }

    private <T> Stream<Student> findStudents(Collection<Student> students,
                                             Function<Student, T> getter, T value) {
        return students.stream().filter(student -> getter.apply(student).equals(value));
    }

    private <T> List<T> getAttributes(Collection<Student> students, Function<Student, T> attribute) {
        return students.stream().map(attribute).collect(Collectors.toList());
    }

    private List<Group> groupsBy(Collection<Student> students, Comparator<Student> comp) {
        return sortedStudentStream.apply(students, comp).collect(Collectors.groupingBy(
                        Student::getGroup, TreeMap::new, Collectors.toList())).entrySet().stream().
                map(group -> new Group(group.getKey(), group.getValue()))
                .collect(Collectors.toList());
    }

    private GroupName getLargestGroup(Collection<Student> students, Function<List<Student>,
            Integer> funcCompare, Comparator<? super GroupName> comparator) {
        return getLargest(students, Student::getGroup, Collectors.toList(), Comparator.comparing(
                        (Map.Entry<GroupName, List<Student>> group) -> funcCompare.apply(group.getValue())).
                thenComparing(Map.Entry.comparingByKey(comparator)), null);
    }

    private <T, D, E> T getLargest(Collection<Student> students, Function<Student, T> attribute,
                                   Collector<Student, E, D> collector, Comparator<Map.Entry<T, D>> comparator, T notFound) {
        return students.stream().collect(Collectors.groupingBy(attribute,
                collector)).entrySet().stream().max(comparator).map(Map.Entry::getKey).orElse(notFound);
    }

    private <T> List<T> getByIds(Collection<Student> students,
                                 int[] ids, Function<Student, T> attribute) {
        return Arrays.stream(ids).mapToObj((id) ->
                        findStudents(students, Student::getId, id).findAny().orElse(null)).
                map(attribute).collect(Collectors.toList());
    }
}