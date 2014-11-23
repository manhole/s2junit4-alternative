# S2JUnit4 alternative

[S2JUnit4](http://s2container.seasar.org/2.4/ja/S2JUnit4.html) 同等の機能を@Ruleで実現するプロダクトです。

## 動作環境

* Java 6
* JUnit 4.11
* S2Tiger 2.4.47


## 使い方

テストクラスのpublicフィールドに、@Ruleで注釈したS2TestRuleを宣言してください。

```
// @RunWith(Seasar2.class) は不要
public class FooTest {

    // S2TestRuleインスタンスをpublicフィールドにし、@Ruleで注釈する。
    @Rule
    public S2TestRule testRule = S2TestRule.create(this); 

    @Test
    public void some() {
        // your tests ...
    }

}
```

## S2JUnit4との違い

* @RunWith(org.junit.runner.RunWith)ではなく、org.junit.rules.TestRuleを使います。
* Parameterizedを使う場合は @RunWith(Parameterized.class) に指定する必要があります。
* TestRuleで実現されていることにより、@Beforeなどの処理順序が変わっています。@Beforeは処理タイミングが変わったためTestContextのセットアップに向かなくなりました。
いままで@Before,beforeで TestContext#setAutoIncluding(false); していた箇所は、書き換える必要があります。

```
private TestContext ctx;

@Before
public void before() {
    ctx.setAutoIncluding(false);
}
```

↓

```
private TestContext ctx;

@BeforeTest
public void before() {
    ctx.setAutoIncluding(false);
}
```


### 移植する機能

* @PostBindFields, @PreUnbindFields アノテーションを利用できるようにします。
* xxxx


### 移植しない機能

#### [メソッド命名規則](http://s2container.seasar.org/2.4/ja/S2JUnit4.html#methodNamingConvention) によるテストメソッド判別。

"beforeClass"などのメソッド名による判別は行いません。
@BeforeClassなど対応するJUnit4アノテーションを使用してください。
テストメソッドには@Testを付けてください。

beforeXxx, afterXxxは、代替アノテーションが存在しないため移植対象にしようか検討しましたが、
@BeforeTest, @AfterTest という代替アノテーションを提供するようにしました。

#### EasyMockサポート

@Mockなどのアノテーションはサポートしません。



## TODO

* validation

例えば @BeforeTest("hoge") は @Test public void hoge() {...} とセットで使うものだが、
対応するhogeメソッドが無い場合にエラーとしたい。


