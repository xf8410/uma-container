package io.github.xf8410.umcontainer

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * PoC 壳界面。容器引擎（engine-core / engine-native）后续模块化接入。
 *
 * 路线图见 README：
 * 1. PoC：游戏 APK 装进容器 → 启动到标题画面
 * 2. 反作弊实测（决定性节点）
 * 3. Android 14/15 适配
 * 4. hlpatch SO 容器内加载
 * 5. umawork 宿主集成
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        val title = TextView(this).apply {
            text = "uma-container\nPoC 0.1.0"
            gravity = Gravity.CENTER
            textSize = 20f
        }
        val status = TextView(this).apply {
            text = getString(R.string.poc_status)
            gravity = Gravity.CENTER
            textSize = 14f
            setPadding(0, 32, 0, 0)
        }
        root.addView(title)
        root.addView(status)
        setContentView(root)
    }
}
