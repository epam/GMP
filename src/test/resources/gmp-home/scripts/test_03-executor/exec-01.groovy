import com.epam.gmp.ScriptResult
import com.epam.gmp.service.ScriptRunner

import java.util.concurrent.Future

ScriptRunner runner = gConfig.runner
Future<ScriptResult<Integer>> result1 = runner.run('0@test_03-executor/exec-00.groovy', Collections.EMPTY_LIST)
Future<ScriptResult<Integer>> result2 = runner.run('1@test_03-executor/exec-00.groovy', Collections.EMPTY_LIST)

return new ScriptResult(result1.get().result + result2.get().result)