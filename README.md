# Android-sticky-navigation-layout
 a lib of android sticky navigation layout , not only contains it, but also contains a lib of 'android-nested-scrolling'.
 
   android 停靠控件， 包含android最新嵌套滑动处理库。 不依赖listView or recyclerView.只要是实现了NestedScrollingChild的控件都可以。
   
  <img src="/art/sticky_navigation_layout.gif" alt="Demo Screen Capture" width="403px" height="677px"/>

## Gradle config


wait...


## 使用说明 wait...

## Android-nestedScroll 库， 说明文档

- IScrollHelper  滑动处理服务的超级接口, 直接子类ScrollHelper.

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
                //  ps: dx,dy 带方向的)，比如dx >0 表示手势向上。
```
                
-  INestedScrollHelper    嵌套滑动处理辅助接口。子类 NestedScrollHelper.
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

## About me
   * heaven7 
   * email: donshine723@gmail.com or 978136772@qq.com   
   
## hope
i like technology. especially the open-source technology.And previous i didn't contribute to it caused by i am a little lazy, but now i really want to do some for the open-source. So i hope to share and communicate with the all of you.


## License

    Copyright 2015   
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
