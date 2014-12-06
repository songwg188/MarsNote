package com.mars.note.views;

/*
 * Author Mars
 * Date 20141202
 * Description 1、refer to http://blog.csdn.net/mydreamongo/article/details/30468543
 *             ViewPager在滑动中有可能touch事件被子View消耗掉，因此重写onInterceptTouchEvent
 *             当距离大于10px就认为是滑动事件，不再给子View分发事件，具体原理参考onTouch事件传递机制
 *             解释为何重写的是onInterceptTouchEvent，因为返回true将不再给子View传递事件
 *             
 *             2、实现边界回弹
 */

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.TranslateAnimation;

public class BounceViewPager extends ViewPager {
	private boolean isScrollable = true;
	private boolean isTouchable = true;
	
	private Rect mRect = new Rect();// 用来记录初始位置
	private int pagerCount = 0;
	private int currentItem = 0;
	private boolean handleDefault = true;
	private static final float RATIO = 0.5f;// 摩擦系数
	private static final float SCROLL_WIDTH = 20f;
	float preX = 0;
	private boolean isSpringback = false;

	public void setSpringBack(boolean b) {
		isSpringback = b;
	}

	public boolean isScrollable() {
		return isScrollable;
	}

	public void setScrollable(boolean isScrollable) {
		this.isScrollable = isScrollable;
	}

	public void setTouchable(boolean enable) {
		isTouchable = enable;
	}

	public BounceViewPager(Context context) {
		super(context);
		init();
	}

	public BounceViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	@Override
	public void scrollTo(int x, int y) {
		if (isScrollable) {
			super.scrollTo(x, y);
		}
	}
	
	private void init(){
		this.setOnPageChangeListener(new OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int arg0) {
				setCurrentIndex(arg0);
				
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub
				
			}
		});		
	}
	
	@Override
	public void setAdapter(PagerAdapter arg0) {
		super.setAdapter(arg0);
		this.setpagerCount(arg0.getCount());
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (isTouchable) {
			boolean result = super.onInterceptTouchEvent(ev);
			if (ev.getAction() == MotionEvent.ACTION_DOWN) {
				preX = ev.getX();
			} else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
				if (Math.abs(ev.getX() - preX) > 4) {
					return true;
				} else {
					preX = ev.getX();
				}
			}
			return result;
		}
		return false;
	}

	// 设置总共有多少页,请记得调用它
	public void setpagerCount(int pagerCount) {
		this.pagerCount = pagerCount;
	}

	// 这是当前是第几页，请在onPageSelect方法中调用它。
	public void setCurrentIndex(int currentItem) {
		this.currentItem = currentItem;
	}

	@Override
	public boolean onTouchEvent(MotionEvent arg0) {
		if (isTouchable) {
			if (isSpringback) {
				switch (arg0.getAction()) {
				case MotionEvent.ACTION_UP:
					onTouchActionUp();
					break;
				case MotionEvent.ACTION_MOVE:
					// 当时滑到第一项或者是最后一项的时候。
					if ((currentItem == 0 || currentItem == pagerCount - 1)) {
						float nowX = arg0.getX();
						float offset = nowX - preX;
						preX = nowX;
						if (currentItem == 0) {
							if (offset > SCROLL_WIDTH) {// 手指滑动的距离大于设定值
								whetherConditionIsRight(offset);
							} else if (!handleDefault) {// 这种情况是已经出现缓冲区域了，手指慢慢恢复的情况
								if (getLeft() + (int) (offset * RATIO) >= mRect.left) {
									layout(getLeft() + (int) (offset * RATIO),
											getTop(), getRight()
													+ (int) (offset * RATIO),
											getBottom());
								}
							}
						} else {
							if (offset < -SCROLL_WIDTH) {
								whetherConditionIsRight(offset);
							} else if (!handleDefault) {
								if (getRight() + (int) (offset * RATIO) <= mRect.right) {
									layout(getLeft() + (int) (offset * RATIO),
											getTop(), getRight()
													+ (int) (offset * RATIO),
											getBottom());
								}
							}
						}
					} else {
						handleDefault = true;
					}

					if (!handleDefault) {
						return true;
					}
					break;

				default:
					break;
				}
			}
			return super.onTouchEvent(arg0);
		}
		return false;
	}

	private void whetherConditionIsRight(float offset) {
		if (mRect.isEmpty()) {
			mRect.set(getLeft(), getTop(), getRight(), getBottom());
		}
		handleDefault = false;
		layout(getLeft() + (int) (offset * RATIO), getTop(), getRight()
				+ (int) (offset * RATIO), getBottom());
	}

	private void onTouchActionUp() {
		if (!mRect.isEmpty()) {
			recoveryPosition();
		}
	}

	private void recoveryPosition() {
		TranslateAnimation ta = null;
		ta = new TranslateAnimation(getLeft(), mRect.left, 0, 0);
		ta.setDuration(300);
		startAnimation(ta);
		layout(mRect.left, mRect.top, mRect.right, mRect.bottom);
		mRect.setEmpty();
		handleDefault = true;
	}
	
	@Override
	public void setCurrentItem(int item) {
		// TODO Auto-generated method stub
		super.setCurrentItem(item);
		setCurrentIndex(item);
	}

}
