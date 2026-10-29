import com.nodus.launcher.home.AppDrawerPolicy as Drawer
import kotlin.math.abs

fun main() {
    var passed = 0
    fun test(name: String, body: () -> Unit) { body(); passed++; println("PASS $name") }
    test("panel follows finger distance and reversal continuously") {
        val first = Drawer.drag(0f, -40f, 800f)
        val second = Drawer.drag(first, -40f, 800f)
        val reversed = Drawer.drag(second, 20f, 800f)
        check(abs(first - .05f) < .0001f && abs(second - .1f) < .0001f)
        check(abs(reversed - .075f) < .0001f)
    }
    test("slow short gesture returns to home") {
        check(!Drawer.opens(.05f, -100f, 800f, 1f))
    }
    test("sufficient distance completes opening even without a fling") {
        check(Drawer.opens(.125f, 0f, 800f, 1f))
        check(Drawer.opens(.21f, 0f, 400f, 1f))
    }
    test("short deliberate upward fling opens") {
        check(Drawer.opens(.05f, -900f, 800f, 1f))
    }
    test("fast downward reversal cancels opening despite earlier distance") {
        check(!Drawer.opens(.5f, 900f, 800f, 1f))
    }
    test("tap noise never opens even with a high velocity estimate") {
        check(!Drawer.opens(.01f, -4000f, 800f, 1f))
        check(!Drawer.opens(0f, -4000f, 800f, 1f))
    }
    test("distance and velocity thresholds scale consistently with density") {
        for (density in listOf(1f, 2f, 3.5f)) {
            check(Drawer.opens(.05f, -900f*density, 800f*density, density))
            check(!Drawer.opens(.05f, -100f*density, 800f*density, density))
        }
    }
    test("bounds and unavailable viewport remain safe") {
        check(Drawer.drag(.9f,-400f,800f)==1f)
        check(Drawer.drag(.1f,400f,800f)==0f)
        check(Drawer.drag(.2f,-400f,0f)==.2f)
        check(!Drawer.opens(1f,-900f,0f,1f))
        check(!Drawer.opens(1f,-900f,800f,0f))
    }
    test("downward pull closes only after intentional distance or fling") {
        check(!Drawer.closes(.99f, 2000f, 800f, 1f))
        check(!Drawer.closes(.95f, 100f, 800f, 1f))
        check(Drawer.closes(.85f, 0f, 800f, 1f))
        check(Drawer.closes(.95f, 900f, 800f, 1f))
        check(!Drawer.closes(.5f, -900f, 800f, 1f))
        for (d in listOf(1f, 2f, 3.5f)) check(Drawer.closes(.95f, 900f*d, 800f*d, d))
        check(!Drawer.closes(0f, 900f, 0f, 1f))
    }
    println("$passed app drawer policy tests passed")
}
