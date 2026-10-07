package com.sheikhtube.app

import android.app.*
import android.os.Bundle
import android.webkit.*
import android.widget.*
import android.content.Context
import android.net.Uri

class MainActivity : Activity() {
 private lateinit var web: WebView
 private lateinit var address: EditText
 private val prefs by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }
 private val blocked = listOf("doubleclick.net","googlesyndication.com","googleadservices.com","adservice.google.com","adsystem.com","scorecardresearch.com","taboola.com","outbrain.com")
 override fun onCreate(b: Bundle?) { super.onCreate(b); setContentView(R.layout.activity_main)
  web=findViewById(R.id.web); address=findViewById(R.id.address)
  web.settings.javaScriptEnabled=true; web.settings.domStorageEnabled=true; web.settings.mediaPlaybackRequiresUserGesture=false
  web.webChromeClient=WebChromeClient()
  web.webViewClient=object:WebViewClient(){
   override fun shouldInterceptRequest(v:WebView?, r:WebResourceRequest?):WebResourceResponse? {
    if(prefs.getBoolean("block",true) && blocked.any { r?.url?.host?.contains(it,true)==true }) return WebResourceResponse("text/plain","utf-8",null)
    return super.shouldInterceptRequest(v,r)
   }
   override fun onPageFinished(v:WebView?, url:String?){ address.setText(url ?: "") }
  }
  findViewById<Button>(R.id.go).setOnClickListener { navigate(address.text.toString()) }
  address.setOnEditorActionListener { _,_,_-> navigate(address.text.toString()); true }
  findViewById<Button>(R.id.back).setOnClickListener { if(web.canGoBack()) web.goBack() }
  findViewById<Button>(R.id.menu).setOnClickListener { showSettings() }
  web.loadUrl("https://m.youtube.com/")
 }
 private fun navigate(s:String){ val q=s.trim(); val url=if(q.startsWith("http://")||q.startsWith("https://")) q else if(q.contains(".")&&!q.contains(" ")) "https://$q" else "https://www.google.com/search?q="+Uri.encode(q); web.loadUrl(url) }
 private fun showSettings(){
  val items=arrayOf(if(prefs.getBoolean("block",true)) "✓ Content blocker: ON" else "Content blocker: OFF", "Clear browsing data", "Home: YouTube")
  AlertDialog.Builder(this).setTitle("Sheikh Tube Settings").setItems(items){_,which-> when(which){
   0->{ val n=!prefs.getBoolean("block",true); prefs.edit().putBoolean("block",n).apply(); Toast.makeText(this,"Content blocker ${if(n) "ON" else "OFF"}",Toast.LENGTH_SHORT).show() }
   1->{ web.clearCache(true); web.clearHistory(); CookieManager.getInstance().removeAllCookies(null); Toast.makeText(this,"Browsing data cleared",Toast.LENGTH_SHORT).show() }
   2->web.loadUrl("https://m.youtube.com/")
  }}.show()
 }
 override fun onBackPressed(){ if(web.canGoBack()) web.goBack() else super.onBackPressed() }
}
