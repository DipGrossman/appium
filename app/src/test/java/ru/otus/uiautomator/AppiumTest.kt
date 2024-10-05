package ru.otus.uiautomator

import de.matthiasmann.twl.utils.PNGDecoder
import io.appium.java_client.AppiumDriver
import io.appium.java_client.MobileElement
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.pagefactory.AppiumFieldDecorator
import io.appium.java_client.remote.MobileCapabilityType
import io.qameta.allure.kotlin.junit4.AllureRunner
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.openqa.selenium.OutputType
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.support.FindBy
import org.openqa.selenium.support.PageFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import java.nio.ByteBuffer
import java.nio.file.Paths

@RunWith(AllureRunner::class)
class AppiumTest {

    private var driver: AppiumDriver<MobileElement>? = null

    @Before
    fun setup() {
        val capabilities = DesiredCapabilities()
        val userDir = System.getProperty("user.dir")        // module dir
        val serverAddress = URL("http://127.0.0.1:4723")
        val localApp = "src/test/res/foodium.apk"
        val appPath = Paths.get(userDir, localApp).toAbsolutePath().toString()
        capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android")
        capabilities.setCapability(MobileCapabilityType.APP, appPath)
        capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, "UiAutomator2")
        capabilities.setCapability("appium:allowInsecure", "adb_shell")
        driver = AndroidDriver(serverAddress, capabilities)
    }

    @After
    fun tearDown() {
        driver?.quit()
    }

    @Test
    fun openDocTest() {
        val page = MainPage(driver!!)
        page.firstDoc?.click()
        page.content?.isDisplayed()
    }

    @Test
    fun shareDocTest() {
        val page = MainPage(driver!!)
        page.firstDoc?.click()
        page.actionShare?.click()
        page.shareTitle?.isDisplayed()
        page.shareBody?.isDisplayed()
    }

    @Test
    fun changeThemeTest() {
        val page = MainPage(driver!!)
        page.themeButton?.click()
        //получаем скриншот и вырезаем из него view
        val bytes = driver!!.getScreenshotAs(OutputType.BYTES)
        val png = PNGDecoder(ByteArrayInputStream(bytes))
        val originalBuffer = ByteBuffer.allocate(png.width * png.height * 3)
        png.decode(originalBuffer, 0, PNGDecoder.Format.RGB)
        val rect = page.screenView?.rect
        val buffer = getSubImage(
            originalBuffer,
            rect?.x ?: 0,
            rect?.y ?: 0,
            rect?.width ?: 0,
            rect?.height ?: 0,
            png.width
        )
        val goldenPath =
            System.getProperty("user.dir") + "/src/test/java/" + javaClass.`package`.name
                .replace(".", File.separator) + "/golden"
        val goldenImgName = "goldenimage1.bin"
        if (!File("$goldenPath/$goldenImgName").exists()) {
            //создадим новое эталонное изображение
            val file = File("$goldenPath/$goldenImgName")
            file.writeBytes(buffer.array())
            return      //golden image was done
        } else {
            val golden = File("$goldenPath/$goldenImgName").readBytes()
            //сравним изображение с эталоном
            assertTrue(golden.mapIndexed { index, byte -> buffer[index] == byte }.all { it })
        }
    }




    @Test
    fun disableNetworkTest() {
        driver?.executeScript("mobile: shell", mapOf("command" to "cmd connectivity airplane-mode enable"))
        val page = MainPage(driver!!)
        page.networkStatus?.isDisplayed()
        driver?.executeScript("mobile: shell", mapOf("command" to "cmd connectivity airplane-mode disable"))
    }
}

fun getSubImage(
    buffer: ByteBuffer,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    originalWidth: Int
): ByteBuffer {
    val result = ByteBuffer.allocate(width * height * 3)    //for RGB
    for (dy in y until y + height) {
        for (dx in x until x + width) {
            result.put(buffer[(dy * originalWidth + dx) * 3 + 0])       //R
            result.put(buffer[(dy * originalWidth + dx) * 3 + 1])       //G
            result.put(buffer[(dy * originalWidth + dx) * 3 + 2])       //B
        }
    }
    return result
}

abstract class Page(d: AppiumDriver<*>) {
    init {
        PageFactory.initElements(AppiumFieldDecorator(d), this)
    }
}
class MainPage(driver: AppiumDriver<*>) : Page(driver) {
    @FindBy(xpath = "//androidx.recyclerview.widget.RecyclerView[@resource-id=\"dev.shreyaspatil.foodium:id/postsRecyclerView\"]/android.view.ViewGroup[1]")
    val firstDoc: MobileElement? = null

    @FindBy(id = "dev.shreyaspatil.foodium:id/action_bar_root")
    val content: MobileElement? = null

    @FindBy(id = "dev.shreyaspatil.foodium:id/action_share")
    val actionShare: MobileElement? = null

    @FindBy(xpath = "//android.widget.RelativeLayout")
    val shareTitle:MobileElement? = null

    @FindBy(id = "android:id/resolver_list")
    val shareBody:MobileElement? = null

    @FindBy(id = "dev.shreyaspatil.foodium:id/networkStatusLayout")
    val networkStatus:MobileElement? = null

    @FindBy(id = "dev.shreyaspatil.foodium:id/action_theme")
    val themeButton:MobileElement? = null

    @FindBy(xpath = "//android.widget.LinearLayout")
    var screenView: MobileElement? = null
}



