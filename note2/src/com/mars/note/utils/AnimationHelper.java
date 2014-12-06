package com.mars.note.utils;

import com.mars.note.Config;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.Animator.AnimatorListener;
import android.view.View;

public class AnimationHelper {
	public static void runRotateAnim(View v, float begin, float end,
			int duration) {
		ObjectAnimator.ofFloat(v, "rotation", begin, end).setDuration(duration)
				.start();
	}

	public static void runVerticalAnim(final View v, float begin, float end,
			int duration, final int visibility) {
		ObjectAnimator anim = ObjectAnimator.ofFloat(v, "y", begin, end)
				.setDuration(duration);
		anim.addListener(new AnimatorListener() {

			@Override
			public void onAnimationCancel(Animator arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animator arg0) {
				// TODO Auto-generated method stub
				v.setVisibility(visibility);
			}

			@Override
			public void onAnimationRepeat(Animator arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationStart(Animator arg0) {
				v.setVisibility(View.VISIBLE);

			}

		});
		anim.start();
	}

	public static void runHorizontalAnim(final View v, float begin, float end,
			int duration, final int visibility) {
		ObjectAnimator anim = ObjectAnimator.ofFloat(v, "x", begin, end)
				.setDuration(duration);
		anim.addListener(new AnimatorListener() {

			@Override
			public void onAnimationCancel(Animator arg0) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onAnimationEnd(Animator arg0) {
				// TODO Auto-generated method stub
				v.setVisibility(visibility);
			}

			@Override
			public void onAnimationRepeat(Animator arg0) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onAnimationStart(Animator arg0) {
				// TODO Auto-generated method stub

			}

		});
		anim.start();
	}

	public static void runAlphaAnim(final View v, float begin, float end,
			int duration) {
		ObjectAnimator anim = ObjectAnimator.ofFloat(v, "alpha", begin, end)
				.setDuration(duration);
		anim.start();
	}

	public static void runScaleAnim(final View v, float begin, float end,
			int duration, AnimatorListener listener) {
		ObjectAnimator anim = ObjectAnimator.ofFloat(v, "scaleX", begin, end);
		ObjectAnimator anim2 = ObjectAnimator.ofFloat(v, "scaleY", begin, end);
		ObjectAnimator anim3 = ObjectAnimator.ofFloat(v, "alpha", 1.0F, 0.1F);
		AnimatorSet set = new AnimatorSet();
		set.playTogether(anim, anim2, anim3);
		set.setDuration(duration);
		set.addListener(listener);
		set.start();
	}

	public static void runHorizontalOutAnim(final View v, float begin,
			float end, int duration) {

		ObjectAnimator anim = ObjectAnimator.ofFloat(v, "x", begin, end);
		ObjectAnimator anim2 = ObjectAnimator.ofFloat(v, "alpha", 1.0F, 0.1F);
		AnimatorSet set = new AnimatorSet();
		set.playTogether(anim, anim2);
		set.setDuration(duration);
//		set.addListener(new AnimatorListener() {
//
//			@Override
//			public void onAnimationCancel(Animator arg0) {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void onAnimationEnd(Animator arg0) {
////				v.setVisibility(View.INVISIBLE);
//
//			}
//
//			@Override
//			public void onAnimationRepeat(Animator arg0) {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void onAnimationStart(Animator arg0) {
//				// TODO Auto-generated method stub
//
//			}
//		});
		set.start();
	}

	public static void runHorizontalInAnim(final View v, float begin,
			float end, int duration) {
		ObjectAnimator anim = ObjectAnimator.ofFloat(v, "x", begin, end);
		ObjectAnimator anim2 = ObjectAnimator.ofFloat(v, "alpha", 0.1F, 1.0F);
		AnimatorSet set = new AnimatorSet();
		set.playTogether(anim, anim2);
		set.setDuration(duration);
		set.start();
	}

	public static void runVerticalInAnim(final View v, float begin, float end,
			int duration) {
		ObjectAnimator anim = ObjectAnimator.ofFloat(v, "y", begin, end);
		ObjectAnimator anim2 = ObjectAnimator.ofFloat(v, "alpha", 0.1F, 1.0F);
		AnimatorSet set = new AnimatorSet();
		set.playTogether(anim, anim2);
		set.setDuration(duration);
		set.addListener(new AnimatorListener() {

			@Override
			public void onAnimationCancel(Animator arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animator arg0) {
				Config.animation_finished = true;
			}

			@Override
			public void onAnimationRepeat(Animator arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationStart(Animator arg0) {
				Config.animation_finished = false;

			}
		});
		set.start();
	}

	public static void runVerticalOutAnim(final View v, float begin, float end,
			int duration) {
		ObjectAnimator anim = ObjectAnimator.ofFloat(v, "y", begin, end);
		ObjectAnimator anim2 = ObjectAnimator.ofFloat(v, "alpha", 1.0F, 0.1F);
		AnimatorSet set = new AnimatorSet();
		set.playTogether(anim, anim2);
		set.setDuration(duration);
		set.addListener(new AnimatorListener() {

			@Override
			public void onAnimationCancel(Animator arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animator arg0) {
				Config.animation_finished = true;

			}

			@Override
			public void onAnimationRepeat(Animator arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationStart(Animator arg0) {
				Config.animation_finished = false;

			}
		});
		set.start();
	}
}
