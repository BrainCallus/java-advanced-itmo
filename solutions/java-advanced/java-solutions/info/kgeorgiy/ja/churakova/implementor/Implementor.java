package info.kgeorgiy.ja.churakova.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;
import org.junit.Assert;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Implementor implements JarImpler {

    /**
     * {@link System#lineSeparator()}
     */
    private static final String SEPARATE = System.lineSeparator();

    /**
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException if given arguments are incorrect @see {@link #checkArgs},
     *                         or if {@link #getHeader}, {@link #getClassBody} thrown {@link ImplerException},
     *                         or if {@link IOException} occur during writing implemented class
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        checkArgs(token, root);
        root = getFullPath(token, root);
        mkDirs(root);
        root = root.resolve(getImplName(token) + ".java");

        try (BufferedWriter writer = Files.newBufferedWriter(root)) {
            writer.write(getHeader(token));
            getClassBody(token, writer);
            writer.write("}" + SEPARATE);
        } catch (IOException e) {
            throw new ImplerException("Writing class " + token + " failed", e);
        }

    }

    /**
     * @param token   type token to create implementation for.
     * @param jarFile target <var>.jar</var> file.
     * @throws ImplerException if given arguments are incorrect @see {@link #checkArgs},
     *                         or if {@link #implement}, {@link #myCompile}, {@link #packJar}, {@link #deleteDir} thrown {@link ImplerException},
     *                         or if {@link IOException} occur during creating directory
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        checkArgs(token, jarFile);

        try {
            Path jarDir = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "jarDir");
            try {
                implement(token, jarDir);
                myCompile(jarDir, token);
                packJar(token, jarFile, jarDir);
            } finally {
                deleteDir(jarDir);
            }
        } catch (IOException e) {
            throw new ImplerException("Can't create directory for file " + jarFile + SEPARATE + e);
        }
    }


    /**
     * Deletes directory
     *
     * @param path {@link Path} to deleting directory
     * @throws ImplerException if {@link IOException} occur during deleting
     */
    private static void deleteDir(Path path) throws ImplerException {

        try (Stream<Path> files = Files.walk(path)) {
            files.map(Path::toFile).forEach(File::delete);
        } catch (IOException e) {
            throw new ImplerException("Can't create stream for deleting files from " + path);
        }
    }


    /**
     * Pack implemented class into jar file.
     * <p>
     * Creating {@link Manifest} and pack class with it into jar file
     *
     * @param token  {@link Class} that implements
     * @param root   {@link Path} to token
     * @param jarDir {@link Path} of directory where implemented class is
     * @throws ImplerException if {@link IOException} occur during creating InputStream or putting
     *                         jar's entries
     */
    private void packJar(Class<?> token, Path root, Path jarDir) throws ImplerException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(root), manifest)) {
            jar.putNextEntry(new JarEntry(token.getPackageName().replace(".", "/")
                    + "/" + token.getSimpleName() + "Impl.class"));

            Files.copy(getAbsolutePath(token, jarDir, "class"), jar);
        } catch (IOException man) {
            throw new ImplerException("Can't create jar file", man);
        }
    }

    /**
     * Compiles implemented class
     *
     * @param root  {@link Path} to class
     * @param token {@link Class} that implemented
     */
    private void myCompile(Path root, Class<?> token) {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        // :NOTE: лучше не использовать Assert из junit, поскольку это не тест. Стоит явно преоверять, например, через if
        Assert.assertNotNull("Could not find java compiler, include tools.jar to classpath", compiler);
        String classpath = getClassPath(token);
        final String[] args = {
                getAbsolutePath(token, root, "java").toString(),
                "-cp",
                classpath,
                "-encoding", StandardCharsets.UTF_8.name()
        };
        final int exitCode = compiler.run(null, null, null, args);
        Assert.assertEquals("Compiler exit code", 0, exitCode);
    }

    /**
     * Return class path to given class
     *
     * @param token {@link Class} which class path get
     * @return class path of token
     * @throws AssertionError if {@link URISyntaxException} occur
     */
    private String getClassPath(Class<?> token) {
        try {
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Writes class body
     * <p>
     * Writes implementing for all declared abstract not final methods @see {@link #implementAbstractMethods}.
     * If given class isn't an interface writes all declared not private constructors @see {@link #implementConstructors}
     *
     * @param token  {@link Class} that body will be implemented
     * @param writer {@link BufferedWriter} that writes implementing parts to class file
     * @throws ImplerException if {@link ImplerException} thrown in @see {@link #implementAbstractMethods}
     *                         or in @see {@link #implementConstructors}
     */
    private void getClassBody(Class<?> token, BufferedWriter writer) throws ImplerException {
        if (isClass(token)) {
            implementConstructors(token, writer);
        }
        implementAbstractMethods(token, writer);
    }

    /**
     * Writes implementing for all declared abstract not final methods
     * <p>
     * If class has no such methods then will be written nothing. If method is abstract and final it won't be written
     *
     * @param token  {@link Class} that abstract methods will be implemented
     * @param writer {@link BufferedWriter} that writes implementing methods to class file
     * @throws ImplerException if {@link ImplerException} thrown during getting methods in @see {@link #getAbstractMethods}
     *                         or in {@link #buildUnit} or if {@link IOException} occur during writing methods
     */
    private void implementAbstractMethods(Class<?> token, BufferedWriter writer) throws ImplerException {
        for (Method met : getAbstractMethods(token)) {
            try {
                writer.write(buildUnit(met));
            } catch (ImplerException | IOException e) {
                throw new ImplerException("Can't implement method " + met + SEPARATE + e);
            }
        }
    }

    /**
     * Define whether given class interface or class.
     * <p>
     * If given class not interface then it is a class
     *
     * @param token {@link Class} that type defines
     * @return boolean value represents that token not interface
     */
    private boolean isClass(Class<?> token) {
        return !token.isInterface();
    }

    /**
     * Check given class for its correctness
     *
     * @param token {@link Class} to check
     * @throws ImplerException if token is null or token is {@link Array},
     *                         {@link javax.lang.model.type.PrimitiveType}, {@link Enum} or if it has final {@link Modifier}
     */
    private void checkClass(Class<?> token) throws ImplerException {
        if (token == null) {
            throw new ImplerException("Class is null");
        }
        if (token.isPrimitive() || token.isArray() ||
                token == Enum.class || Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Can't implement given class because of it's incorrect type");
        }

    }

    /**
     * Check given arguments for its correctness
     *
     * @param token {@link Class}
     * @param root  {@link Path} to class
     * @throws ImplerException - if path is null or @see {@link #checkClass} trown exception
     *                         because of token incorrectness
     */
    private void checkArgs(Class<?> token, Path root) throws ImplerException {
        if (root == null) {
            throw new ImplerException("Path is null");
        }
        checkClass(token);
    }

    /**
     * Make and return name for implementing class
     *
     * @param token {@link Class}
     * @return string consists of given class name + "Impl"
     */
    private String getImplName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Returns path to package contains class without fileName.
     * <p>
     * This method equals to getAbsolutePath.getParent().
     * In given parameter path all dots replaced with {@link File#separator}
     *
     * @param token {@link Class} to witch path will get
     * @param path  {@link Path} that will be resolved with class package name
     * @return {@link Path} to package where token is
     */
    private Path getFullPath(Class<?> token, Path path) {
        return path.resolve(token.getPackageName().replace(".", File.separator));
    }

    /**
     * Returns absolute path to file contains class with class name to which added "Impl"
     *
     * @param token     {@link Class} to witch path will get
     * @param path      {@link Path} that will be resolved with class package name
     * @param extension string representing file extension to witch path get
     * @return {@link Path} to file where token is
     */
    private Path getAbsolutePath(Class<?> token, Path path, String extension) {
        return getFullPath(token, path).resolve(getImplName(token) + "." + extension);
    }

    /**
     * Creates directories for given path
     *
     * @param path {@link Path} where directories will be created
     * @throws ImplerException if {@link SecurityException} or {@link IOException}
     *                         during creating directories occur
     */
    private void mkDirs(Path path) throws ImplerException {
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path);
            } catch (IOException md) {
                throw new ImplerException("Can not create directories for path " + path + "\n" + md.getMessage());
            }
        }
    }

    /**
     * Build and returns string representing information about package,
     * inheriting and name replaced to name+"Impl" for given class;
     *
     * @param token {@link Class}
     * @return string representing class header
     * @throws ImplerException if given class is private
     */
    private String getHeader(Class<?> token) throws ImplerException {
        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Can't implement private class " + token);
        }
        String header = "";
        if (!token.getPackageName().isEmpty()) {
            header += String.format("package %s;%n%n", token.getPackageName());

        }
        header += String.format("public class %s %s%s {%n", getImplName(token),
                isClass(token) ? "extends " : "implements ", token.getCanonicalName());

        return header;
    }

    /**
     * Filters objects from given array and returns {@link Stream} of them
     *
     * @param mets   an array of objects that extends {@link Executable}
     * @param filter {@link Function} from {@link Integer} to {@link Boolean} filters mets
     * @param <E>    type of elements in mets, extends Executable
     * @return {@link Stream} of Executable filtered by filter
     */
    private <E extends Executable> Stream<E> getFilteredStream(E[] mets, Function<Integer, Boolean> filter) {
        return Arrays.stream(mets).filter(met -> filter.apply(met.getModifiers()));
    }

    /**
     * Return concatenation of given method's name({@link Method#getName}),
     * return type({@link Method#getReturnType}) and its parameters return types({@link Method#getParameterTypes})
     *
     * @param met {@link Method}
     * @return string represents met's name, return type and its parameters return types
     */
    private String keyMethodFeatures(Method met) {
        return String.format("%s%s%s", met.getName(), met.getReturnType(),
                Arrays.toString(met.getParameterTypes()));
    }

    /**
     * Returns {@link Map} with String key and {@link Method} value
     * <p>
     * Key string in Map represents concatenation of significant method's features @see {@link #keyMethodFeatures}
     *
     * @param methods an array of {@link Method}
     * @param filter  {@link Function} from {@link Integer} to {@link Boolean} filters methods
     * @return {@link Map} where string key represents method name, return type and parameter return types and the value is {@link Method}
     */
    private Map<String, Method> getMethodsSet(Method[] methods, Function<Integer, Boolean> filter) {
        return getFilteredStream(methods, filter)
                .collect(Collectors.toMap(this::keyMethodFeatures, met -> met));
    }

    /**
     * Returns set of all abstract not final methods that has class.
     * <p>
     * If class has no such methods returns empty set; if method is abstract and final it won't be return
     *
     * @param token {@link Class} that methods will return
     * @return {@link Set} of  {@link Method} for token
     */
    private Set<Method> getAbstractMethods(Class<?> token) {

        Map<String, Method> methods = getMethodsSet(token.getMethods(), Modifier::isAbstract);
        Map<String, Method> excluded = getMethodsSet(token.getDeclaredMethods(), Modifier::isFinal);
        while (token != null) {
            methods.putAll(getMethodsSet(token.getDeclaredMethods(), Modifier::isAbstract));
            token = token.getSuperclass();

        }
        excluded.keySet().forEach(methods::remove);
        return new HashSet<>(methods.values());
    }

    /**
     * From array of objects by mapping gets and returns string of values obtained
     * using parameter mapper delimited with coma
     *
     * @param elements an array of objects on the basis of which it is built returnable string
     * @param mapper   {@link Function} from elements type to string
     * @param <E>      type of elements
     * @return string of values obtained using the mapper substrings delimited with coma
     */
    private <E> String getListOfElements(E[] elements, Function<E, String> mapper) {
        return Arrays.stream(elements).map(mapper).collect(Collectors.joining(","));
    }

    /**
     * Build string contains information about exceptions that Executable throws
     *
     * @param met instance of {@link Executable}
     * @param <E> type of parameter met, extends Executable
     * @return string contains information about exceptions that Executable throws;
     * if Executable throws no exceptions returns empty string
     */
    private <E extends Executable> String getMethodExceptions(E met) {
        String exceptions = "";
        if (met.getExceptionTypes().length != 0) {
            exceptions += String.format(" throws %s", getListOfElements(met.getExceptionTypes(),
                    Class::getCanonicalName));

        }
        return exceptions;
    }

    /**
     * Returns default value for given {@link Class}
     *
     * @param token {@link Class} of Method's returnType that was got in @see {@link #buildUnitBody}
     * @return string consists of default type for given class :
     * "" - if given class equals void. class, "false" - if equals boolean,
     * "0"  - if equals other primitives, "null" - otherwise
     */
    private String getDefaultValue(Class<?> token) {
        if (token.equals(void.class)) {
            return "";
        } else if (token.equals(boolean.class)) {
            return "false";
        } else if (token.isPrimitive()) {
            return "0";
        }
        return "null";
    }

    /**
     * Writes implementing {@link Class} all declared {@link Constructor} that has not private {@link Modifier}
     *
     * @param token  implementing {@link Class}
     * @param writer {@link BufferedWriter} writes implementing class
     * @throws ImplerException if implementing class has no not private {@link Constructor}
     *                         or if in called method inside @see {@link #buildUnit} {@link ImplerException} occur
     *                         or if {@link IOException} occur during writing class constructors
     */
    private void implementConstructors(Class<?> token, BufferedWriter writer) throws ImplerException {
        Set<Constructor<?>> constructors = getFilteredStream(token.getDeclaredConstructors(), this::modifierNotPrivate)
                .collect(Collectors.toSet());
        if (constructors.isEmpty()) {
            throw new ImplerException("Not found constructors for class " + token);
        }
        for (Constructor<?> constructor : constructors) {
            try {
                writer.write(buildUnit(constructor));
            } catch (IOException ec) {
                throw new ImplerException("Can't implement constructor " + constructor + " for class " + token);
            }
        }
    }

    /**
     * @param value result of call to {@link Executable#getModifiers()} on {@link Executable} instance
     * @return inverted value of {@link Modifier#isPrivate}
     */
    private boolean modifierNotPrivate(int value) {
        return !Modifier.isPrivate(value);
    }

    /**
     * Returns a string representing an implementation of instance of {@link Executable}(constructor or method)
     *
     * @param unit instance of {@link Executable}
     * @param <E>  type of parameter unit, extends Executable
     * @return string representing implementation of Executable
     * @throws ImplerException if in @see {@link #getMethodParameters} {@link MalformedParametersException} occur
     */
    private <E extends Executable> String buildUnit(E unit) throws ImplerException {
        return String.format("    %s %s%s%s {%n%s;%n }%n",
                getMethodModifs(unit), buildUnitDeclaring(unit),
                getMethodParameters(unit, true), getMethodExceptions(unit),
                buildUnitBody(unit));
    }

    /**
     * Builds executable part of the {@link Executable}
     *
     * @param unit instance of Executable
     * @param <E>  type of parameter unit, extends Executable
     * @return string of Executable({@link Constructor} or {@link Method} body that consists
     * of "return" + default value of Executable declared return type, if it is instance of  {@link Method},
     * and of call to constructor of unit superClass, if it is instance of  {@link Constructor}
     * @throws ImplerException if Executable is instance of  {@link Constructor} and
     *                         {@link MalformedParametersException} occur because of its Parameters incorrectness
     */
    private <E extends Executable> String buildUnitBody(E unit) throws ImplerException {
        if (unit instanceof Method) {
            return "    return ".concat(getDefaultValue(((Method) unit).getReturnType()));
        }
        return "    super ".concat(getMethodParameters(unit, false));
    }

    /**
     * Builds declaring of the {@link Executable}
     *
     * @param unit instance of Executable
     * @param <E>  type of parameter unit, extends Executable
     * @return declaration of Executable: return's type canonical name and method's name, if unit instance of {@link Method},
     * and class name, if unit instance of {@link Constructor}
     */
    private <E extends Executable> String buildUnitDeclaring(E unit) {
        if (unit instanceof Method) {
            return methodDeclaring((Method) unit);
        }
        return getImplName(unit.getDeclaringClass());
    }

    /**
     * Builds declaring of the {@link Method}
     *
     * @param unit {@link Method} that declaration returns
     * @return string method declaration: return's type canonical name and method's name
     */
    private String methodDeclaring(Method unit) {
        return unit.getReturnType().getCanonicalName().concat(" ").concat(unit.getName());
    }

    /**
     * Get modifiers of the {@link Executable}
     *
     * @param met instance of {@link Executable}
     * @param <E> type of parameter met, extends Executable
     * @return string with met's {@link Modifier}:
     */
    private <E extends Executable> String getMethodModifs(E met) {
        return Modifier.toString(met.getModifiers()
                & ~Modifier.NATIVE & ~Modifier.TRANSIENT & ~Modifier.ABSTRACT);
    }

    /**
     * Get parameters that given {@link Constructor} or {@link Method} accepts
     *
     * @param met          instance of {@link Executable}
     * @param requireTypes boolean value points whether method need parameters with their types
     * @param <E>          type of parameter met, extends Executable
     * @return string of list of parameters that have given Executable
     * that starts with open scope and ends with close scope
     * @throws ImplerException if given Executable has incorrect parameters led to {@link MalformedParametersException}
     */
    private <E extends Executable> String getMethodParameters(E met, boolean requireTypes) throws ImplerException {
        try {
            return "(" + getListOfElements(met.getParameters(),
                    requireTypes ? this::getSingleParam : Parameter::getName) + ")";
        } catch (MalformedParametersException m) {
            throw new ImplerException("Incorrect parameters in " + met);
        }
    }

    /**
     * Return string representing parameter's type and name
     *
     * @param parameter input {@link Parameter}
     * @return string consists of the CanonicalName of the given parameter's type, whitespace and parameter's Name
     */
    private String getSingleParam(Parameter parameter) {
        return parameter.getType().getCanonicalName() + " " + parameter.getName();
    }

    /**
     * Checks whether given arguments are correct.
     * <p>
     * All arguments shouldn't be not null and their length of arguments must be 2 or 3;
     * in case 3 arguments the first equal to "-jar" expected
     *
     * @param args parameters inputs from commandline
     * @return true if arguments correct, false - otherwise
     */
    private static boolean verifyArgs(String[] args) {
        if (args == null) {
            System.err.println("Args are null");
            return false;
        } else if (args.length != 2 && args.length != 3) {
            System.err.println("Required 2 or 3 arguments, but given " + args.length);
            return false;
        }
        for (String arg : args) {
            if (arg == null) {
                System.err.println("Arguments can't be null");
                return false;
            }
        }
        if (args.length == 3 && !args[0].equals("-jar")) {
            System.err.println("If you want to implementJar, first argument must be \"-jar\"");
            return false;
        }
        return true;
    }

    /**
     * Implements class and if first argument is "-jar" pack it into jar file.
     * <p>
     * If given arguments considered as incorrect or errors occurs during implementation, print error message
     *
     * @param args arguments from commandline
     */
    public static void main(String[] args) {
        if (!verifyArgs(args)) {
            return;
        }

        Implementor implementor = new Implementor();
        try {
            if (args.length == 2) {
                implementor.implement(Class.forName(args[0]), Path.of(args[1]));
            } else {
                implementor.implementJar(Class.forName(args[1]), Path.of(args[2]));
            }

        } catch (InvalidPathException ip) {
            System.err.println("Path " + args[1] + " to " + args[0] + " is invalid" + SEPARATE + ip.getMessage());
        } catch (ImplerException impl) {
            System.err.println("Can't implement " + args[0] + " because an exception occur during implementation" + SEPARATE + impl.getMessage());
        } catch (ClassNotFoundException c) {
            System.err.println("Not found class " + (args.length == 2 ? args[0] : args[1]) + SEPARATE + c.getMessage());
        }
    }
}
