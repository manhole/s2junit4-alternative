# S2JUnit4 alternative INSIDE document

## 処理順について

オリジナルのS2JUnit4ではRunnerで実現されています。
S2JUnit4 alternativeではTestRuleで実現します。
実現方法の違いにより、処理順序が変わっています。

S2JUnit4では:

1. @Before, before()
1. @PostBindFields
1. @Test
1. @PreUnbindFields
1. @After, after()


S2JUnit4 alternativeでは:

1. @PostBindFields
1. (Statement#evaluate ...)
1. @Before
1. @Test
1. @After
1. (... Statement#evaluate)
1. @PreUnbindFields


### 処理順を詳細に記述すると...

S2JUnit4では:

1. (TestContext初期化)
1. @Before, before() ... TestContextセットアップに使われている
1. beforeXxx()
1. (S2Container初期化)
1. (bindFields)
1. @PostBindFields
1. (トランザクション開始)
1. @Test
1. (トランザクション終了)
1. @PreUnbindFields
1. (unbindFields)
1. (S2Container破棄)
1. afterXxx()
1. @After, after()
1. (TestContext破棄)


S2JUnit4 alternativeでは:

1. (TestContext初期化)
1. **@BeforeTest**        ... NEW: @Before, before()の代わり。TestContextをセットアップするならここ。
1. **@BeforeTest("xxx")** ... NEW: beforeXxx()の代わり
1. (S2Container初期化)
1. (bindFields)
1. @PostBindFields
1. (トランザクション開始)
1. (Statement#evaluate ...)
1. @Before                ... CHANGED
1. @Test
1. @After                 ... CHANGED
1. (... Statement#evaluate)
1. (トランザクション終了)
1. @PreUnbindFields
1. (unbindFields)
1. (S2Container破棄)
1. **@AfterTest("xxx")**  ... NEW: afterXxx()の代わり
1. **@AfterTest**         ... NEW: @After, after()の代わり
1. (TestContext破棄)


## MEMO

#### 実現方式をTestRuleにした理由は?

Runner方式にしてしまうと、他のRunnerを使用できないためです。
(Runnderを指定する@RunWithアノテーションは、テストクラスに1つのみ使用できます。)


#### @Before, @AfterのタイミングがS2JUnit4と異なる理由は?

実現方式をTestRuleにしたことが理由です。

Runnerは広い範囲の処理フローを制御することが可能ですが、TestRuleはその処理フローの一部であるためです。
(TestRuleでは、Statement#evaluateの内部(ここで@Before,@Afterが動く)へは手出しができません。)


#### タイミングが変わって気をつける点は?

TestContextをセットアップするのは、@BeforeTestで行うようにしてください。

@BeforeTestはS2Container初期化前ですので、S2Container#registerなどの前処理があればここで行ってください。

(S2JUnit4では@Before,before()がS2Container初期化前のタイミングでしたが、alternativeでは@BeforeTestが代わりとなります。)
