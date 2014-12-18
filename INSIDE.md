# S2JUnit4 alternative INSIDE document

## 処理順について

S2JUnit4ではRunnerで実現していました。
alternativeではTestRuleで実現します。
実現方法の違いにより、処理順序が変わっています。

S2JUnit4では:

1. @Before, before()
1. @PostBindFields
1. @Test
1. @PreUnbindFields
1. @After, after()


TestRuleで実装すると:

1. @PostBindFields
1. (Statement#evaluate ...)
1. @Before
1. @Test
1. @After
1. (... Statement#evaluate)
1. @PreUnbindFields

Statement#evaluateの内部へは手出しができません。
@PostBindFieldsと@Beforeが、@PreUnbindFieldsと@Afterが同じ状態(bindされた状態)で実行されることになります。

困ること:
S2Container初期化前にTestContextをセットアップしたい。
S2JUnit4では@Before,before()のタイミングがそうだったが、TestRuleではタイミングがない。


処理順を詳細に記述すると...

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


TestRuleで実装すると:

1. (TestContext初期化)
1. **@BeforeTest**        ... NEW: @Before, before()の代わり
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

