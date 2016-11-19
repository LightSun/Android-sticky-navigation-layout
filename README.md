# Android-sticky-navigation-layout
 a lib of android sticky navigation layout , not only contains it, but also contains a lib of 'android-nested-scrolling'.
 
   android 停靠控件， 包含android最新嵌套滑动处理库。 不依赖listView or recyclerView.只要是实现了NestedScrollingChild的控件都可以。
   
  <img src="/art/sticky_navigation_layout.gif" alt="Demo Screen Capture" width="403px" height="677px"/>

## Gradle config

```java
   dependencies {
        //android 嵌套滑动处理库
        compile 'com.heaven7.android.scroll:android-nestedscroll:1.0'
        //sticky-navigation-layout 
        wait...
   }
```


## sticky-navigation-layout  使用步骤：

---原理。
```java
     停靠控件分成3个部分:  头，head view 
                         停靠: navigation view
                         随手势滚动控件： recyclerView或者其他。
```
                         
                      
使用指南：  (demo中已包含复杂的业务或者说布局， 如果仍有问题，可提issue.)

- 1,在xml中添加 停靠控件。如下图.
```java
 <com.heaven7.android.StickyLayout.StickyNavigationLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        android:id="@+id/stickyLayout"
        android:layout_above="@+id/rg"
        app:stickyLayout_top_id = "@+id/top_view"   
        app:stickyLayout_indicator_id = "@+id/vp_indicator"
        app:stickyLayout_content_id = "@+id/rv"
        >

        <com.heaven7.android.StickyLayout.NestedScrollFrameLayout
            android:id="@+id/top_view"
            android:layout_width="match_parent"
            android:layout_height="200dp"
            android:background="#8800ff00"
            >

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_centerVertical="true"
                android:layout_gravity="center"
                android:gravity="center"
                android:text="标题党--嘎嘎"
                android:textSize="30sp"
                android:textStyle="bold" />
        </com.heaven7.android.StickyLayout.NestedScrollFrameLayout>

        <com.heaven7.android.sticky_navigation_layout.demo.view.SimpleViewPagerIndicator
            android:id="@id/vp_indicator"
            android:layout_width="match_parent"
            android:layout_height="50dp"
            android:background="#88000000" />

        <android.support.v7.widget.RecyclerView
            android:id="@id/rv"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:background="#44ff0000"
            android:scrollbars="vertical"
            >
        </android.support.v7.widget.RecyclerView>

    </com.heaven7.android.StickyLayout.StickyNavigationLayout>
    
```
 具体见demo,  其中：
```java
/**
       stickyLayout_top_id         表示 head view 的id.
       stickyLayout_indicator_id   表示 navigation view 的id.
       stickyLayout_content_id     表示 下面内容的 view 的 id. 
       其余还有2个属性:
             stickyLayout_auto_fit_scroll       滑动完手离开时，是否自动平滑到指定位置, 默认false.
                                                具体的距离根据 stickyLayout_threshold_percent 来确定。
             stickyLayout_threshold_percent     自动平滑到位置的 百分比.float类型。不写默认是0.5          
                                   
*/
```
- NestedScrollFrameLayout 
        是自定义的另外一个支持嵌套滑动的控件。如果你想你的Header view支持嵌套滑动。
        用此控件即可。



## Android-nestedScroll 库， 说明文档

- 简述

   随着google写了一套嵌套滑动的处理规范， NestedScrollingChild 和 NestedScrollingParent. 但是我们要怎么去实现呢？
   
   比如我们要写一个支持停靠的自定义控件。而且可内嵌SwipeRefreshLayout和 RecyclerView.那么用嵌套滑动规范就是
   
   最佳选择。 因为最新的SwipeRefreshLayout和 RecyclerView都针对嵌套滑动做了支持。 我这套库就可以极大简化代码。
   
   具体可以见 StickyNavigationLayout 源码。 所以处理嵌套滑动用Android-nestedScroll库就对啦. 
   
   使用 Android-nestedScroll 后的处理 滑动 以及 嵌套滑动 的核心代码：
   
```java
     @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(!isNestedScrollingEnabled()){
            return super.onInterceptTouchEvent(ev);
        }
        return mNestedHelper.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!isNestedScrollingEnabled()){
            return super.onTouchEvent(event);
        }
        return mNestedHelper.onTouchEvent(event);
    }
    @Override
    public void computeScroll() {
        mNestedHelper.computeScroll();
    }
      @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        mNestedHelper.nestedScroll(dx, dy, consumed, true);

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }
    ...
```
   
   下面介绍一下 Android-nestedScroll 库.

- IScrollHelper  滑动处理服务的超级接口, 实现类ScrollHelper.

```java
     滑动状态的3个常量: 
        空闲：       SCROLL_STATE_IDLE 
        拖拽中：     SCROLL_STATE_DRAGGING 
        setting中：  SCROLL_STATE_SETTLING （一般用于fling 或者 平滑 smoothScroll）

     //设置滑动状态，内部会自动通知状态改变
    void setScrollState(int state);
    //获取当前的滑动状态 
    int getScrollState();

    //通知滑动距离改变
    void dispatchOnScrolled(int dx, int dy);
    
    //按照给定的增量x和y滑动
    void scrollBy(int dx, int dy);

    //滑动到指定的x和y
    void scrollTo(int x, int y);
    
    //按照指定的增量平滑
    void smoothScrollBy(int  dx, int dy);
    //平滑到指定的x 和 y
    void smoothScrollTo(int x, int y);
    
    // 停止滑动
    void stopScroll();

    //同view的 computeScroll, 主要是在自定义控件时，通过重写View的computeScroll方法， 调用此方法即可.
    void computeScroll();
   
    //按照指定的x轴速度和 y轴速度 fling.
    boolean fling(float velocityX, float velocityY);

    //添加滑动监听器
    void addOnScrollChangeListener(OnScrollChangeListener l);
    
    //移除滑动监听器
    void removeOnScrollChangeListener(OnScrollChangeListener l);
 
    //是否包含该滑动监听器
    boolean hasOnScrollChangeListener(OnScrollChangeListener l);   

```
-  内部类： OnScrollChangeListener 滑动监听器 (包含状态监听和距离监听)。 
```java
    来源： View类也有。不过从api 23开始才有通用的, 以前listView,recyclerView都是单独的监听器。
    
    方法： void onScrollStateChanged(View target, int state);  状态改变回调
    
           void onScrolled(View target, int dx, int dy);   滑动距离回调 
                //  ps: dx,dy 带方向的)，比如dy > 0 表示 手势向上。
```
                
-  INestedScrollHelper    嵌套滑动处理辅助接口。实现类 NestedScrollHelper.
```java 

    //设置是否启用嵌套滑动, 同NestedScrollingChildHeloer.setNestedScrollingEnabled. 只不过增加状态改变回调
    void setNestedScrollingEnabled(boolean enable);
    
    //是否启用了嵌套滑动
    boolean isNestedScrollingEnabled();

    
    //复写 {@link android.view.ViewGroup#onInterceptTouchEvent(MotionEvent)} 调用此方法, 支持嵌套滑动. 具体见 demo实现。                 
    boolean onInterceptTouchEvent(MotionEvent ev);

   
    //复写 {@link android.view.ViewGroup#onTouchEvent(MotionEvent)} 调用此方法 ,支持嵌套滑动 
    boolean onTouchEvent(MotionEvent event);

     //通过指定的增量 执行嵌套滑动  ，返回true如果执行了
    boolean nestedScrollBy(int dx, int dy, MotionEvent ev);

    
     /*真正执行嵌套滑动，
     // @param dx      x增量
     //@param dy      y增量
     //@param consumed  x 和 y轴 消耗的滑动的距离。可选参数
     // @param dispatchScroll 是否通知滑动距离改变
     //@return 返回x ,y 消耗的滑动距离  
     */
    int[] nestedScroll(int dx, int dy, int[] consumed, boolean dispatchScroll);
    
```

- 滑动处理相关的回调 ScrollCallback 和 NestedScrollCallback 。（非OnScrollChangeListener监听器）

```java
       //水平方向是否可滑动。如果支持水平滑动返回true
        public abstract boolean canScrollHorizontally(View target);
  
        //竖直是否支持滑动
        public abstract boolean canScrollVertically(View target);

        //获取x轴最大的滑动距离 
        public int getMaximumXScrollDistance(View target) ;

        //获取y轴最大的滑动距离
        public int getMaximumYScrollDistance(View target);
        
        //滑动距离回调。默认空实现
        public void onScrolled(int dx, int dy);
```        

- NestedScrollFactory  创建ScrollHelper和 NestedScrollHelper对象的工厂

## About me
   * heaven7 
   * email: donshine723@gmail.com or 978136772@qq.com   
   
## hope
i like technology. especially the open-source technology.And previous i didn't contribute to it caused by i am a little lazy, but now i really want to do some for the open-source. So i hope to share and communicate with the all of you.


## License

    Copyright 2016  
                    heaven7(donshine723@gmail.com)

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
