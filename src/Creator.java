import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;

public class Creator {
    private static final String CLASS_NAME = "MyClass";

    public static void task(String ifElse) throws Exception {
        //загружаем компилятор
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        String str = "flag = true;";
        int index = ifElse.indexOf('{');
        StringBuffer sb = new StringBuffer(ifElse);
        sb.insert(index+1, str);

        ifElse = String.valueOf(sb)      ;


        String code = "" +
                "public class MyClass {" +
                "    public static void main(String[] args) {" +
                "boolean flag =false;" +
                ifElse +
                "Resulter.setResult(result, flag);" +
                "    }" +
                "}";

        //исходный код
        SourceCode sourceCode = new SourceCode(CLASS_NAME, code);

        //скомпилированный байт-код
        CompiledCode compiledCode = new CompiledCode(CLASS_NAME);

        //переопределяем метод поиска класса для загрузки
        ClassLoader classLoader = new ClassLoader(ClassLoader.getSystemClassLoader()) {
            @Override
            protected Class<?> findClass(String name) {
                //записывем байт-код откомпилированного класса
                byte[] byteCode = compiledCode.getByteCode();
                //получаем представление класса
                return defineClass(name, byteCode, 0, byteCode.length);
            }
        };

        //данные только в оперативной памяти
        JavaFileManager fileManager = new ForwardingJavaFileManager<>(compiler.getStandardFileManager(null, null, null)) {
            //в качестве выходного файла будет вызван наш объект
            @Override
            public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
                return compiledCode;
            }

            @Override
            public ClassLoader getClassLoader(Location location) {
                return classLoader;
            }
        };

        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, null, null, Collections.singletonList(sourceCode));

        if (task.call()) {
            Class<?> clazz = classLoader.loadClass(CLASS_NAME);
            clazz.getDeclaredMethod("main", String[].class).invoke(null, new Object[]{null});
        }
    }


    /**
     * Класс для хранения исходного кода на языке Java.
     */
    static class SourceCode extends SimpleJavaFileObject {
        private String code;

        SourceCode(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    /**
     * Класс для хранения скомпилированного байт-кода.
     */
    static class CompiledCode extends SimpleJavaFileObject {
        private ByteArrayOutputStream baos = new ByteArrayOutputStream();

        CompiledCode(String className) throws Exception {
            super(new URI(className), Kind.CLASS);
        }

        @Override
        public OutputStream openOutputStream() {
            return baos;
        }

        byte[] getByteCode() {
            return baos.toByteArray();
        }
    }
}
