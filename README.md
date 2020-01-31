弹性圆点指示器添加

1.在build.gradle(project)添加如下内容  
allprojects {  
		repositories {  
			...
			maven { url 'https://jitpack.io' }  
		}  
}  

2.在build.gradle(app)添加如下内容即可  
dependencies {  
	        implementation 'com.github.PYJTLK:StickyIndicator:1.0'  
}  


弹性圆点指示器使用方法  

设置布局文件  
<com.example.administrator.viewtest.StickyIndicator  
        android:id="@+id/indicator"  
        android:layout_width="match_parent"  
        android:layout_height="wrap_content"  
        .../>  
          
属性列表:
        app:allowAnim     是否允许有弹性动画效果 eg:true  
        app:count     圆点/长条的个数  eg:5  
        app:currentIndex    当前圆点/长条的位置 eg:0  
        app:radius   圆点的半径  eg:10dp   需要配置app:style为stroke或fill  
        app:color   当前圆点/长条的颜色  eg:#A00A  
        app:style   风格   strip/fill/stroke  
        app:interval    圆点/长条的间隔   eg:30dp    在layout_width="wrap_content"时才有效  
        app:hideFlash   是否隐藏闪光效果  eg:false  
        app:hideBack    是否隐藏未选圆点/长条  eg:false  
        app:backColor   未选圆点/长条的颜色    eg:#A00  
        app:stripWidth    长条的长度   eg:20dp    需要配置app:style为strip  
        app:stripHeight   长条的高度   eg:5dp     需要配置app:style为strip  
        app:indicatorClickable    圆点/长条是否可以点击   eg:false  
        


eg:  
  <com.example.stickyindicator.StickyIndicator  
        android:id="@+id/indicator"  
        android:layout_width="match_parent"  
        android:layout_height="wrap_content"  
        app:count="6"  
        app:radius="20dp"  
        app:color="#A00A"  
        app:style="stroke"  
        app:backColor="#A0A0"/>  
