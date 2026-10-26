# Test Data

Sample sources for manual and integration tests of `ptx-codeinfo`. Unit test fixtures
for edge cases stay in `src/test/resources/fixtures/`.

| File | Total | Code | Comment | Empty | Covers |
| ---- | ----- | ---- | ------- | ----- | ------ |
| `Sample.java` | 26 | 15 | 6 | 5 | Block and line comments, trailing comment, comment markers in a string and a text block |

`MainTest.testRunLocOnTestdataSample` checks these counts. Update the table and the
test together when you change a sample.

```bash
java -jar target/ptx-codeinfo.jar loc --verbose testdata/Sample.java
```
