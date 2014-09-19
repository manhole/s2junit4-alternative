package jp.sourceforge.hotchpotch.s2junit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import javax.transaction.TransactionManager;

import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.seasar.framework.beans.BeanDesc;
import org.seasar.framework.beans.factory.BeanDescFactory;
import org.seasar.framework.container.S2Container;
import org.seasar.framework.container.factory.S2ContainerFactory;
import org.seasar.framework.container.factory.SingletonS2ContainerFactory;
import org.seasar.framework.container.impl.S2ContainerBehavior;
import org.seasar.framework.convention.NamingConvention;
import org.seasar.framework.convention.impl.NamingConventionImpl;
import org.seasar.framework.env.Env;
import org.seasar.framework.exception.NoSuchMethodRuntimeException;
import org.seasar.framework.unit.EasyMockSupport;
import org.seasar.framework.unit.InternalTestContext;
import org.seasar.framework.unit.S2TestIntrospector;
import org.seasar.framework.unit.Seasar2;
import org.seasar.framework.unit.UnitClassLoader;
import org.seasar.framework.unit.annotation.PublishedTestContext;
import org.seasar.framework.unit.impl.ConventionTestIntrospector;
import org.seasar.framework.util.DisposableUtil;
import org.seasar.framework.util.ResourceUtil;
import org.seasar.framework.util.StringUtil;
import org.seasar.framework.util.tiger.CollectionsUtil;
import org.seasar.framework.util.tiger.ReflectionUtil;

/**
 * @author manhole
 */
public class S2TestRule implements TestRule {

    /**
     * S2JUnit4のデフォルトの設定ファイルのパス
     */
    protected static final String DEFAULT_S2JUNIT4_PATH = "s2junit4.dicon";

    /**
     * S2JUnit4の設定ファイルのパス
     */
    protected static String s2junit4Path = DEFAULT_S2JUNIT4_PATH;

    /**
     * テストオブジェクト
     */
    protected Object test;

    /**
     * テストクラス
     */
    protected Class<?> testClass;

    /**
     * テストメソッド
     */
    protected Method method;

    /**
     * テストのディスクリプション
     */
    protected Description description;

    /**
     * テストクラスのイントロスペクター
     */
    protected S2TestIntrospector introspector;

    /**
     * {@link #unitClassLoader テストで使用するクラスローダー}で置き換えられる前のオリジナルのクラスローダー
     */
    protected ClassLoader originalClassLoader;

    /**
     * テストで使用するクラスローダー
     */
    protected UnitClassLoader unitClassLoader;

    /**
     * S2JUnit4の内部的なテストコンテキスト
     */
    protected InternalTestContext testContext;

    /**
     * バインディングが行われたフィールドのリスト
     */
    private List<Field> boundFields = CollectionsUtil.newArrayList();

    /**
     * EasyMockとの対話をサポートするオブジェクト
     */
    protected EasyMockSupport easyMockSupport = new EasyMockSupport();

    /**
     * テストが失敗したことを表すフラグ
     */
    protected boolean testFailed;

    private final BeanDesc beanDesc;
    private Statement base;

    private TestContextSetup testContextSetup;

    public S2TestRule(final Object testInstance) {
        this.test = testInstance;
        this.testClass = testInstance.getClass();
        beanDesc = BeanDescFactory.getBeanDesc(testClass);
    }

    public static S2TestRule create(final Object testInstance) {
        return new S2TestRule(testInstance);
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        this.base = base;
        introspector = new ConventionTestIntrospector();
        final String methodName = javaMethodName(description.getMethodName());
        this.method = findMethod(methodName);

        final Steps steps = setupSteps();
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                steps.run();
            }
        };
    }

    private Steps setupSteps() {
        final Steps steps = new Steps();
        steps.enqueue(new StepSkeleton() {
            @Override
            protected void before() throws Throwable {
                Env.setFilePath(Seasar2.ENV_PATH);
                Env.setValueIfAbsent(Seasar2.ENV_VALUE);
            }

            @Override
            protected void after() {
                Env.initialize();
            }
        });

        steps.enqueue(new StepSkeleton() {
            @Override
            protected void before() throws Throwable {
                setUpTestContext();
            }

            @Override
            protected void after() {
                tearDownTestContext();
            }
        });

        steps.enqueue(new StepSkeleton() {
            @Override
            protected void before() throws Throwable {
                runEachBefore();
            }

            @Override
            protected void after() {
                runEachAfter();
            }
        });

        steps.enqueue(new StepSkeleton() {
            @Override
            protected void before() throws Throwable {
                initContainer();
            }

            @Override
            protected void after() {
                destroyContainer();
            }
        });

        steps.enqueue(new StepSkeleton() {
            @Override
            protected void before() throws Throwable {
                testContext.registerColumnTypes();
            }

            @Override
            protected void after() {
                testContext.revertColumnTypes();
            }
        });

        steps.enqueue(new StepSkeleton() {
            @Override
            protected void before() throws Throwable {
                bindFields();
            }

            @Override
            protected void after() {
                try {
                    unbindFields();
                } catch (final Throwable th) {
                    throw new AssertionError(th);
                }
            }
        });

        steps.enqueue(new Step() {
            @Override
            public void step(final Completion completion) throws Throwable {
                runTest();
                completion.complete();
            }
        });
        return steps;
    }

    /*
     * Parameterized.TestClassRunnerForParametersでは、method名を"aaa[0]"のように添字を付けている。
     */
    String javaMethodName(final String methodName) {
        final int length = methodName.length();
        for (int i = 0; i < length; i++) {
            final char c = methodName.charAt(i);
            if (!Character.isJavaIdentifierPart(c)) {
                return methodName.substring(0, i);
            }
        }
        return methodName;
    }

    private Method findMethod(final String methodName) {
        Method found = null;
        final Method[] methods = beanDesc.getMethods(methodName);
        for (final Method m : methods) {
            if (m.getAnnotation(Test.class) != null) {
                if (found != null) {
                    // 同名の@Testメソッドが複数あったらエラー
                    throw new AssertionError();
                }
                found = m;
            }
        }
        return found;
    }

    protected void runTest() throws Throwable {
        if (!testContext.isJtaEnabled()) {
            executeMethod();
            return;
        }
        TransactionManager tm = null;
        if (introspector.needsTransaction(testClass, method)) {
            try {
                tm = testContext.getComponent(TransactionManager.class);
                tm.begin();
            } catch (Throwable t) {
                System.err.println(t);
            }
        }
        try {
            testContext.prepareTestData();
            executeMethod();
        } finally {
            if (tm != null) {
                if (requiresTransactionCommitment()) {
                    tm.commit();
                } else {
                    tm.rollback();
                }
            }
        }
    }

    protected boolean requiresTransactionCommitment() {
        return !testFailed
                && introspector
                .requiresTransactionCommitment(testClass, method);
    }

    protected void executeMethod() throws Throwable {
        base.evaluate();
    }

    // copy from: S2TestMethodRunner
    protected void setUpTestContext() throws Throwable {
        originalClassLoader = getOriginalClassLoader();
        unitClassLoader = new UnitClassLoader(originalClassLoader);
        Thread.currentThread().setContextClassLoader(unitClassLoader);
        if (needsWarmDeploy()) {
            S2ContainerFactory.configure("warmdeploy.dicon");
        }
        final S2Container container = createRootContainer();
        SingletonS2ContainerFactory.setContainer(container);
        testContext = InternalTestContext.class.cast(container
                .getComponent(InternalTestContext.class));
        testContext.setTestClass(testClass);
        testContext.setTestMethod(method);
        if (!testContext.hasComponentDef(NamingConvention.class)
                && introspector.isRegisterNamingConvention(testClass, method)) {
            final NamingConvention namingConvention = new NamingConventionImpl();
            testContext.register(namingConvention);
            testContext.setNamingConvention(namingConvention);
        }

        for (Class<?> clazz = testClass; clazz != Object.class; clazz = clazz
                .getSuperclass()) {

            final Field[] fields = clazz.getDeclaredFields();
            for (int i = 0; i < fields.length; ++i) {
                final Field field = fields[i];
                final Class<?> fieldClass = field.getType();
                if (isAutoBindable(field)
                        && fieldClass.isAssignableFrom(testContext.getClass())
                        && fieldClass
                        .isAnnotationPresent(PublishedTestContext.class)) {
                    field.setAccessible(true);
                    if (ReflectionUtil.getValue(field, test) != null) {
                        continue;
                    }
                    bindField(field, testContext);
                }
            }
        }
        if (testContextSetup != null) {
            testContextSetup.setup(testContext);
        }
    }

    protected void tearDownTestContext() {
        testContext = null;
        DisposableUtil.dispose();
        S2ContainerBehavior
                .setProvider(new S2ContainerBehavior.DefaultProvider());
        Thread.currentThread().setContextClassLoader(originalClassLoader);
        unitClassLoader = null;
        originalClassLoader = null;
    }

    protected void runEachBefore() {
        try {
            final Method eachBefore = introspector.getEachBeforeMethod(
                    testClass, method);
            if (eachBefore != null) {
                invokeMethod(eachBefore);
            }
        } catch (final Throwable e) {
            // TODO
            throw new AssertionError(e);
        }
        easyMockSupport.bindMockFields(test, testContext.getContainer());
    }

    /**
     * テストケース個別の解放メソッドを実行します。
     */
    protected void runEachAfter() {
        easyMockSupport.unbindMockFields(test);
        try {
            final Method eachAfter = introspector.getEachAfterMethod(testClass,
                    method);
            if (eachAfter != null) {
                invokeMethod(eachAfter);
            }
        } catch (final Throwable e) {
            // TODO
            throw new AssertionError(e);
        }
    }

    protected void initContainer() {
        testContext.include();
        introspector.createMock(method, test, testContext);
        testContext.initContainer();
    }

    protected void destroyContainer() {
        testContext.destroyContainer();
    }

    protected void bindFields() throws Throwable {
        for (Class<?> clazz = testClass; clazz != Object.class; clazz = clazz
                .getSuperclass()) {
            final Field[] fields = clazz.getDeclaredFields();
            for (int i = 0; i < fields.length; ++i) {
                bindField(fields[i]);
            }
        }
        List<Method> postBindFieldsMethods = introspector
                .getPostBindFieldsMethods(testClass);
        for (Method m : postBindFieldsMethods) {
            m.invoke(test);
        }
    }

    protected void bindField(final Field field) {
        if (isAutoBindable(field)) {
            field.setAccessible(true);
            if (ReflectionUtil.getValue(field, test) != null) {
                return;
            }
            final String name = resolveComponentName(field);
            Object component = null;
            if (testContext.hasComponentDef(name)) {
                component = testContext.getComponent(name);
                if (component != null) {
                    Class<?> componentClass = component.getClass();
                    if (!field.getType().isAssignableFrom(componentClass)) {
                        component = null;
                    }
                }
            }
            if (component == null
                    && testContext.hasComponentDef(field.getType())) {
                component = testContext.getComponent(field.getType());
            }
            if (component != null) {
                bindField(field, component);
            }
        }
    }

    protected void bindField(final Field field, final Object object) {
        ReflectionUtil.setValue(field, test, object);
        boundFields.add(field);
    }

    protected boolean isAutoBindable(final Field field) {
        final int modifiers = field.getModifiers();
        return !Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)
                && !field.getType().isPrimitive();
    }

    protected String resolveComponentName(final Field filed) {
//        if (testContext.isEjb3Enabled()) {
//            final EJB ejb = filed.getAnnotation(EJB.class);
//            if (ejb != null) {
//                if (!StringUtil.isEmpty(ejb.beanName())) {
//                    return ejb.beanName();
//                } else if (!StringUtil.isEmpty(ejb.name())) {
//                    return ejb.name();
//                }
//            }
//        }
        return normalizeName(filed.getName());
    }

    protected String normalizeName(final String name) {
        return StringUtil.replace(name, "_", "");
    }

    protected ClassLoader getOriginalClassLoader() {
        S2Container configurationContainer = S2ContainerFactory
                .getConfigurationContainer();
        if (configurationContainer != null
                && configurationContainer.hasComponentDef(ClassLoader.class)) {
            return ClassLoader.class.cast(configurationContainer
                    .getComponent(ClassLoader.class));
        }
        return Thread.currentThread().getContextClassLoader();
    }

    protected S2Container createRootContainer() {
        final String rootDicon = introspector.getRootDicon(testClass, method);
        if (StringUtil.isEmpty(rootDicon)) {
            return S2ContainerFactory.create(s2junit4Path);
        }
        S2Container container = S2ContainerFactory.create(rootDicon);
        S2ContainerFactory.include(container, s2junit4Path);
        return container;
    }

    protected void invokeMethod(final Method method) throws Throwable {
        try {
            ReflectionUtil.invoke(method, test);
        } catch (NoSuchMethodRuntimeException ignore) {
        }
    }

    protected void unbindFields() throws Throwable {
        List<Method> preUnbindFieldsMethods = introspector
                .getPreUnbindFieldsMethods(testClass);
        for (Method m : preUnbindFieldsMethods) {
            m.invoke(test);
        }
        for (final Field field : boundFields) {
            try {
                field.set(test, null);
            } catch (IllegalArgumentException e) {
                System.err.println(e);
            } catch (IllegalAccessException e) {
                System.err.println(e);
            }
        }
        boundFields = null;
    }

    protected boolean needsWarmDeploy() {
        return introspector.needsWarmDeploy(testClass, method)
                && !ResourceUtil.isExist("s2container.dicon")
                && ResourceUtil.isExist("convention.dicon")
                && ResourceUtil.isExist("creator.dicon")
                && ResourceUtil.isExist("customizer.dicon");
    }

    public void setupTestContext(final TestContextSetup setup) {
        this.testContextSetup = setup;
    }

    static class Steps {

        private List<Step> steps = new ArrayList<Step>();
        private int position = -1;
        private final Completion completion = new Completion() {
            @Override
            public void complete() throws Throwable {
                nextStep();
            }
        };

        public void enqueue(final Step step) {
            steps.add(step);
        }

        public void run() throws Throwable {
            nextStep();
        }

        protected void nextStep() throws Throwable {
            position++;
            if (position < steps.size()) {
                final Step step = steps.get(position);
                step.step(completion);
            }
        }

    }

    static interface Completion {
        void complete() throws Throwable;
    }

    static interface Step {
        void step(Completion completion) throws Throwable;
    }

    abstract static class StepSkeleton implements Step {

        @Override
        public void step(final Completion completion) throws Throwable {
            before();
            try {
                completion.complete();
            } finally {
                after();
            }
        }

        abstract protected void before() throws Throwable;

        abstract protected void after();

    }

}
