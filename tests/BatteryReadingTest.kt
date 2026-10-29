import com.nodus.launcher.widgets.BatteryReading as Battery

fun main() {
    var passed = 0
    fun test(name: String, body: () -> Unit) { body(); passed++; println("PASS $name") }
    test("fresh broadcast wins over older property or retained state") {
        check(Battery.resolve(62,100,61,60)==62)
        check(Battery.resolve(175,200,null,null)==88)
    }
    test("initial snapshot absent uses direct battery capacity") {
        check(Battery.resolve(-1,-1,96,null)==96)
    }
    test("incomplete broadcasts never erase a valid reading") {
        check(Battery.resolve(-1,100,null,96)==96)
        check(Battery.resolve(96,-1,null,96)==96)
        check(Battery.resolve(96,0,null,96)==96)
    }
    test("unsupported capacity sentinels retain the last valid percentage") {
        for (unsupported in listOf(Int.MIN_VALUE,Int.MAX_VALUE,-1,101)) check(Battery.resolve(-1,-1,unsupported,80)==80)
    }
    test("zero and full battery are real values rather than missing data") {
        check(Battery.resolve(0,100,null,95)==0)
        check(Battery.resolve(-1,-1,0,95)==0)
        check(Battery.resolve(100,100,null,95)==100)
    }
    test("inconsistent broadcast bounds fall back without exceeding 100 percent") {
        check(Battery.resolve(150,100,72,70)==72)
        check(Battery.resolve(-2,100,72,70)==72)
    }
    test("unavailable hardware never produces a fabricated percentage") {
        check(Battery.resolve(-1,-1,null,null)==null)
        check(Battery.resolve(-1,-1,Int.MIN_VALUE,-1)==null)
    }
    println("$passed battery reading tests passed")
}
