import com.epam.dep.esp.common.OS
import com.epam.gmp.ScriptResult


def os = OS.getOs()
def out = new ArrayList()
def result
if (os == OS.win)
    result = os.execCommandLine(['cmd.exe', '/c', 'dir'], out, '.', 30)
logger.info "$result"
logger.info out.join("\n\r")
Thread.currentThread().sleep(1000)

return new ScriptResult<Integer>(1)