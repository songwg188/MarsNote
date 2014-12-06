package com.mars.note.views;
import java.util.Calendar;
import java.util.Date;
import com.mars.note.Config;
import com.mars.note.NoteApplication;
import com.mars.note.R;
import com.mars.note.database.NoteDataBaseManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
public class NoteCalendar extends View {
	private int nums_of_row = 6;
	private int nums_of_columns = 7;
	private final static float STARTX = 0;
	private final static float STARTY = 0;
	private float gridWidth;
	private float gridHeight;
	private float titleGridHeight;
	private float titleGridWidth;
	private int backgroundColor = Color.TRANSPARENT;
	private boolean showGridRowLine = false;
	private boolean showGridColumnLine = false;
	private int currentMonthEndX;
	private int currentMonthEndY;
	private final int currentMonthTextColor = Color.DKGRAY;
	private final int otherMonthTextColor = Color.LTGRAY;
	private final int countColor = Color.BLACK;
	private final int selectedCellBG = Color.RED;
	private final int currentMonthTextSize = 50;
	private final int otherMonthTextSize = 43;
	private boolean touchable = true;
	public boolean isTouchable() {
		return touchable;
	}
	public void setTouchable(boolean touchable) {
		this.touchable = touchable;
	}
	private NoteDataBaseManager mNoteDataBaseManager;
	// List<Cell> monthDates;
	Cell[][] currentMonthArray;
	int[] record_count;
	// curent date
	private int Year; // actual year
	public void setYear(int year) {
		Year = year;
	}
	public void setMonth(int month) {
		Month = month;
	}
	public void setDayOfMonth(int dayOfMonth) {
		DayOfMonth = dayOfMonth;
	}
	private int Month; // actual month
	private int DayOfMonth; // actual day
	private int selectedYear;
	public int getSelectedYear() {
		return selectedYear;
	}
	public int getSelectedMonth() {
		return selectedMonth;
	}
	public int getSelectedDay() {
		return selectedDay;
	}
	private int selectedMonth;
	private int selectedDay;
	private int todayYear;
	private int todayMonth;
	private int todayDay;
	public int SLIDING_LENGTH = 300;
	Paint paint;
	int row1, row2, row3, column1, column2, column3;
	boolean notOutOfRange = true;
	float begin, end;
	ChangeDateListener mChangeDateListener;
	public void setSelectedDay(int year, int month, int dayOfMonth) {
		this.selectedYear = year;
		this.selectedMonth = month;
		this.selectedDay = dayOfMonth;
		if (mChangeDateListener != null) {
			mChangeDateListener.onChangeSelectedDate(selectedYear,
					selectedMonth, selectedDay);
		}
	}
	public NoteCalendar(Context context) {
		super(context);
		paint = new Paint();
		setCurrentDate();
		initNoteDataBaseManager(context);
	}
	private void initNoteDataBaseManager(Context context) {
		mNoteDataBaseManager = NoteApplication.getDbManager();
	}
	private void setCurrentDate() {
		Calendar cal = Calendar.getInstance();
		this.Year = cal.get(Calendar.YEAR);
		this.Month = cal.get(Calendar.MONTH) + 1;
		this.DayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
		this.todayYear = this.Year;
		this.todayMonth = this.Month;
		this.todayDay = this.DayOfMonth;
	}
	
	public void setOnChangeDateListener(ChangeDateListener c) {
		mChangeDateListener = c;
	}
	
	public NoteCalendar(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.NoteCalendar);
		backgroundColor = a.getColor(R.styleable.NoteCalendar_backgroundColor,
				backgroundColor);
		a.recycle();
		paint = new Paint();
		setCurrentDate();
		initNoteDataBaseManager(context);
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		// Log.d("screen", "view width = " + getWidth());
		// Log.d("screen", "view width = " + getHeight());
		caculateCurrentMonthDates();
		caculateOtherMonthDates();
		gridHeight = this.getHeight() / (this.nums_of_row + 0.5F);
		gridWidth = this.getWidth() / this.nums_of_columns;
		titleGridWidth = gridWidth;
		titleGridHeight = (this.getHeight() / (this.nums_of_row + 0.5F)) * 0.5F;
		drawCalendarBG(canvas);
		drawCalendarEdge(canvas);
		drawWeeks(canvas);
		drawRowsAndColumns(canvas);
		drawDayOfMonth(canvas);
		super.onDraw(canvas);
	}
	public void drawDayOfMonth(Canvas canvas) {
		for (int i = 0; i < nums_of_row; i++) {
			for (int j = 0; j < nums_of_columns; j++) {
				if (currentMonthArray[i][j] != null) {
					if (currentMonthArray[i][j].isCurrentDayOfMonth == true) {
						if (currentMonthArray[i][j].year == this.todayYear
								&& currentMonthArray[i][j].month == this.todayMonth
								&& currentMonthArray[i][j].dayOfMonth == this.todayDay) {
							drawCellBG(canvas, i, j,
									Color.parseColor("#44FFFFFF"), true);
							if (selectedYear == todayYear
									&& selectedMonth == todayMonth
									&& selectedDay == todayDay) {
								paint.reset();
								paint.setStyle(Style.FILL);
								paint.setColor(selectedCellBG);
								paint.setAntiAlias(true);
								paint.setAlpha(90);
								paint.setAntiAlias(true);
								// canvas.drawCircle(j * gridWidth + 5 +
								// 10+(gridWidth/2-15),
								// titleGridHeight + 5 + i * gridHeight+
								// 10+((0.5F * gridHeight + (i + 1)* gridHeight
								// - 15)-(titleGridHeight + i * gridHeight+
								// 15))/2 , gridHeight/2, paint);
								// canvas.drawRect(j * gridWidth + 5 + 10,
								// titleGridHeight + 5 + i * gridHeight
								// + 10, (j + 1) * gridWidth - 5
								// - 10,
								// 0.5F * gridHeight + (i + 1)
								// * gridHeight - 5 - 10, paint);
								canvas.drawCircle(
										j * gridWidth + 5 + (gridWidth - 10)
												/ 2,
										((0.5F * gridHeight + (i + 1)
												* gridHeight - 5) - (titleGridHeight + 5 + i
												* gridHeight))
												/ 2
												+ titleGridHeight
												+ 5
												+ i
												* gridHeight,
										(gridHeight - 20) / 2, paint);
							}
							paint.reset();
							paint.setTextSize(currentMonthTextSize);
							paint.setTextAlign(Align.CENTER);
							paint.setColor(currentMonthTextColor);
							paint.setAntiAlias(true);
							paint.setTypeface(Typeface.DEFAULT_BOLD);
							canvas.drawText(
									String.valueOf(currentMonthArray[i][j].dayOfMonth),
									STARTX + j * titleGridWidth + 0.5F
											* gridWidth, STARTY
											+ titleGridHeight + (i + 0.65F)
											* gridHeight, paint);
						} else if (currentMonthArray[i][j].year == this.selectedYear
								&& currentMonthArray[i][j].month == this.selectedMonth
								&& currentMonthArray[i][j].dayOfMonth == this.selectedDay) {
							drawCellBG(canvas, i, j, selectedCellBG, false);
							paint.reset();
							paint.setTextSize(currentMonthTextSize);
							paint.setTextAlign(Align.CENTER);
							paint.setColor(Color.WHITE);
							paint.setAntiAlias(true);
							paint.setTypeface(Typeface.DEFAULT_BOLD);
							canvas.drawText(
									String.valueOf(currentMonthArray[i][j].dayOfMonth),
									STARTX + j * titleGridWidth + 0.5F
											* gridWidth, STARTY
											+ titleGridHeight + (i + 0.65F)
											* gridHeight, paint);
						} else {
							// drawCellBG(canvas, i, j, Color.argb(200, 200,
							// 220, 230));
							paint.reset();
							paint.setTextSize(currentMonthTextSize);
							paint.setTextAlign(Align.CENTER);
							paint.setColor(currentMonthTextColor);
							paint.setAntiAlias(true);
							paint.setTypeface(Typeface.DEFAULT_BOLD);
							canvas.drawText(
									String.valueOf(currentMonthArray[i][j].dayOfMonth),
									STARTX + j * titleGridWidth + 0.5F
											* gridWidth, STARTY
											+ titleGridHeight + (i + 0.65F)
											* gridHeight, paint);
						}
						if (currentMonthArray[i][j].record_count != 0) {
							paint.reset();
							paint.setTextSize(34);
							paint.setTextAlign(Align.CENTER);
							paint.setColor(countColor);
							paint.setTypeface(Typeface.DEFAULT_BOLD);
							paint.setAntiAlias(true);
							canvas.drawText(
									String.valueOf(currentMonthArray[i][j].record_count),
									STARTX + j * titleGridWidth + 0.75F
											* gridWidth, STARTY
											+ titleGridHeight + (i + 0.3F)
											* gridHeight, paint);
						}
					} else {
						paint.reset();
						paint.setTextSize(otherMonthTextSize);
						paint.setTextAlign(Align.CENTER);
						paint.setColor(otherMonthTextColor);
						paint.setAntiAlias(true);
						paint.setTypeface(Typeface.DEFAULT_BOLD);
						canvas.drawText(String
								.valueOf(currentMonthArray[i][j].dayOfMonth),
								STARTX + j * titleGridWidth + 0.5F * gridWidth,
								STARTY + titleGridHeight + (i + 0.65F)
										* gridHeight, paint);
					}
				}
			}
		}
	}
	public void drawCellBG(Canvas canvas, int x, int y, int color, boolean today) {
		paint.reset();
		paint.setStyle(Style.FILL);
		paint.setColor(color);
		paint.setAntiAlias(true);
		paint.setAlpha(100);
		if (today) {
			// canvas.drawRect(y * gridWidth + 5, titleGridHeight + 5 + x
			// * gridHeight, (y + 1) * gridWidth - 5, 0.5F * gridHeight
			// + (x + 1) * gridHeight - 5, paint);
			canvas.drawCircle(
					y * gridWidth + 5 + (gridWidth - 10) / 2,
					((0.5F * gridHeight + (x + 1) * gridHeight - 5) - (titleGridHeight + 5 + x
							* gridHeight))
							/ 2 + titleGridHeight + 5 + x * gridHeight,
					(gridHeight - 10) / 2, paint);
		} else {
			canvas.drawCircle(
					y * gridWidth + 5 + (gridWidth - 10) / 2,
					((0.5F * gridHeight + (x + 1) * gridHeight - 5) - (titleGridHeight + 5 + x
							* gridHeight))
							/ 2 + titleGridHeight + 5 + x * gridHeight,
					(gridHeight - 10) / 2, paint);
		}
	}
	public void drawWeeks(Canvas canvas) {
		paint.reset();
		paint.setStrokeWidth(1);
		canvas.drawLine(0, STARTY + titleGridHeight, this.nums_of_columns
				* titleGridWidth, STARTY + titleGridHeight, paint);
		String[] weekTitles = this.getContext().getResources()
				.getStringArray(R.array.week_titles);
		paint.reset();
		paint.setTextSize(45);
		paint.setColor(Color.DKGRAY);
		paint.setTypeface(Typeface.DEFAULT_BOLD);
		paint.setTextAlign(Align.CENTER);
		for (int i = 0; i < this.nums_of_columns; i++) {
			canvas.drawText(weekTitles[i], STARTX + i * titleGridWidth + 0.5F
					* titleGridWidth, (STARTY + titleGridHeight) * 0.7F, paint);
		}
	}
	public void drawRowsAndColumns(Canvas canvas) {
		paint.reset();
		paint.setStrokeWidth(2);
		paint.setColor(Color.DKGRAY);
		// drawRows
		if (showGridRowLine) {
			for (int i = 0; i < this.nums_of_columns - 2; i++) {
				canvas.drawLine(0, STARTY + titleGridHeight + (i + 1)
						* gridHeight, this.nums_of_columns * gridWidth, STARTY
						+ titleGridHeight + (i + 1) * gridHeight, paint);
			}
		}
		if (showGridColumnLine) {
			for (int h = 0; h < this.nums_of_columns - 1; h++) {
				canvas.drawLine(STARTX + (h + 1) * gridWidth, STARTY, STARTX
						+ (h + 1) * gridWidth, STARTY
						+ (this.nums_of_row + 0.5F) * gridHeight, paint);
			}
		}
		// paint.setColor(Color.BLACK);
		// canvas.drawLine(0, STARTY, this.nums_of_columns * gridWidth, STARTY,
		// paint);
	}
	public void drawCalendarEdge(Canvas canvas) {
		paint.reset();
		paint.setColor(Color.DKGRAY);
		paint.setStrokeWidth(3);
		paint.setStyle(Style.STROKE);
		canvas.drawRect(0, 0, this.getWidth(), this.getHeight(), paint);
	}
	public void drawCalendarBG(Canvas canvas) {
		paint.reset();
		paint.setStyle(Style.FILL);
		paint.setColor(backgroundColor);
		canvas.drawRect(0, 0, this.getWidth(), this.getHeight(), paint);
	}
	public int getDayOfWeek(int year, int month, int dayOfMonth) {
		Date date = new Date(year - 1900, month - 1, dayOfMonth);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
		if (dayOfWeek == 0) {
			return 7;
		}
		return dayOfWeek;
	}
	public int getDaysOfMonth(int year, int month) {
		Calendar test = Calendar.getInstance();
		test.set(Calendar.YEAR, year);
		test.set(Calendar.MONTH, month - 1);
		int totalDay = test.getActualMaximum(Calendar.DAY_OF_MONTH);
		return totalDay;
	}
	public void caculateOtherMonthDates() {
		// caculate previous month
		Calendar previous = Calendar.getInstance();
		int previousMonth = 0;
		int previousYear = 0;
		if (this.Month == 1) {
			previousMonth = 12;
			previousYear = this.Year - 1;
		} else {
			previousMonth = this.Month - 1;
			previousYear = this.Year;
		}
		previous.set(Calendar.YEAR, previousYear); // actual year
		previous.set(Calendar.MONTH, previousMonth - 1); // actual month - 1
		int dayOfPreviousMonth = getDaysOfMonth(previousYear, previousMonth) + 1;
		// Log.d("test","previousYear = "+previousYear);
		// Log.d("test","previousMonth = "+previousMonth);
		// Log.d("test","dayOfPreviousMonth = "+(dayOfPreviousMonth-1));
		int dayOfWeekOfFirstDayOfMonth = getDayOfWeek(this.Year, this.Month, 1);
		for (int i = dayOfWeekOfFirstDayOfMonth - 2; i >= 0; i--) {
			dayOfPreviousMonth--;
			Cell cell = new Cell();
			cell.year = previousYear;
			cell.month = previousMonth;
			cell.dayOfMonth = dayOfPreviousMonth;
			currentMonthArray[0][i] = cell;
		}
		// caculate next month
		int dayOfNextMonth = 0;
		int nextMonth = 0;
		int nextYear = 0;
		if (this.Month == 12) {
			nextMonth = 1;
			nextYear = this.Year + 1;
		} else {
			nextMonth = this.Month + 1;
			nextYear = this.Year;
		}
		boolean begin = false;
		if (this.currentMonthEndX == 5) {
			for (int j = this.currentMonthEndY; j < this.currentMonthArray[5].length; j++) {
				dayOfNextMonth++;
				Cell cell = new Cell();
				cell.year = nextYear;
				cell.month = nextMonth;
				cell.dayOfMonth = dayOfNextMonth;
				currentMonthArray[5][j] = cell;
			}
		} else {
			for (int i = this.currentMonthEndX; i < this.currentMonthArray.length; i++) {
				for (int j = 0; j < this.currentMonthArray[i].length; j++) {
					if (i < this.currentMonthArray.length - 1
							&& j == this.currentMonthEndY) {
						dayOfNextMonth++;
						Cell cell = new Cell();
						cell.year = nextYear;
						cell.month = nextMonth;
						cell.dayOfMonth = dayOfNextMonth;
						currentMonthArray[i][j] = cell;
						begin = true;
					} else {
						if (begin) {
							dayOfNextMonth++;
							Cell cell = new Cell();
							cell.year = nextYear;
							cell.month = nextMonth;
							cell.dayOfMonth = dayOfNextMonth;
							currentMonthArray[i][j] = cell;
						}
					}
				}
			}
		}
	}
	public void caculateCurrentMonthDates() {
		int dayOfWeekOfFirstDayOfMonth = getDayOfWeek(this.Year, this.Month, 1);
		int totalDaysOfMonth = getDaysOfMonth(this.Year, this.Month);
		int dayOfMonth = 0;
		// Log.d("time", "dayOfWeek = " + dayOfWeekOfFirstDayOfMonth);
		// Log.d("time", "getDaysOfMonth = " + totalDaysOfMonth);
		record_count = mNoteDataBaseManager.getCurrentMonthRecordCount(
				this.Year, this.Month, totalDaysOfMonth);
		// for(int i = 0 ; i<record_count.length;i++){
		// Log.d("db","["+(i+1)+"] = "+record_count[i]);
		// }
		boolean begin = false;
		// Log.d("time", "start");
		currentMonthArray = new Cell[nums_of_row][nums_of_columns];
		// monthDates = new ArrayList<Cell>();
		boolean flag = true;
		for (int i = 0; i < this.currentMonthArray.length; i++) {
			if (!flag) {
				break;
			}
			for (int j = 0; j < this.currentMonthArray[i].length; j++) {
				if (i == 0 && (j + 1) == dayOfWeekOfFirstDayOfMonth) {
					dayOfMonth = dayOfMonth + 1;
					Cell cell = new Cell();
					cell.year = this.Year;
					cell.month = this.Month;
					cell.dayOfMonth = dayOfMonth;
					cell.isCurrentDayOfMonth = true;
					cell.record_count = record_count[(dayOfMonth - 1)];
					currentMonthArray[i][j] = cell;
					begin = true;
				} else {
					if (begin) {
						if (dayOfMonth < totalDaysOfMonth) {
							dayOfMonth = dayOfMonth + 1;
							Cell cell = new Cell();
							cell.year = this.Year;
							cell.month = this.Month;
							cell.dayOfMonth = dayOfMonth;
							cell.isCurrentDayOfMonth = true;
							cell.record_count = record_count[(dayOfMonth - 1)];
							currentMonthArray[i][j] = cell;
						} else {
							currentMonthEndX = i;
							currentMonthEndY = j;
							flag = false;
							break;
						}
					}
				}
			}
		}
		for (int i = 0; i < this.currentMonthArray.length; i++) {
			for (int j = 0; j < this.currentMonthArray[i].length; j++) {
				// Log.d("time", "currentMonthArray[" + (i + 1) + "][" + (j + 1)
				// + "] = " + currentMonthArray[i][j]);
			}
		}
		// Log.d("time", "finish");
	}
	private class Cell {
		public int year;
		public int month;
		public int dayOfMonth;
		boolean isCurrentDayOfMonth;
		public int record_count;
	}
	private void touchToChangeDate(float x, float y) {
		int row = (int) ((y - this.titleGridHeight) / this.gridHeight);
		int column = (int) (x / this.gridWidth);
//		android.util.Log.d("touch", "row = " + row);
//		android.util.Log.d("touch", "column = " + column);
	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			begin = event.getX();
//			android.util.Log.d("touch", "begin = " + begin);
			if (event.getY() > this.titleGridHeight) {
				row1 = (int) ((event.getY() - this.titleGridHeight) / this.gridHeight);
				column1 = (int) (event.getX() / this.gridWidth);
				// android.util.Log.d("touch","row1 = "+(row1+1));
				// android.util.Log.d("touch","column1 = "+(column1+1));
			}
		}
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			row2 = (int) ((event.getY() - this.titleGridHeight) / this.gridHeight);
			column2 = (int) (event.getX() / this.gridWidth);
			if (row2 != row1 || column2 != column1) {
				this.notOutOfRange = false;
			}
			// float end = event.getX();
			// if ((end - begin) > SLIDING_LENGTH) {
			// this.setBackgroundResource(R.color.white3);
			// } else if ((begin - end) > SLIDING_LENGTH) {
			// // this.setBackgroundResource(R.color.gray3);
			// this.setBackgroundResource(R.color.white3);
			// } else {
			// this.setBackgroundResource(R.color.transparent);
			// }
		}
		if (event.getAction() == MotionEvent.ACTION_UP) {
			this.setBackgroundResource(R.color.transparent);
			if (event.getY() > this.titleGridHeight && touchable) {
				row3 = (int) ((event.getY() - this.titleGridHeight) / this.gridHeight);
				column3 = (int) (event.getX() / this.gridWidth);
				// android.util.Log.d("touch","row2 = "+(row2+1));
				// android.util.Log.d("touch","column2 = "+(column2+1));
				if (notOutOfRange && row1 == row3 && column1 == column3) {
					// android.util.Log.d("touch", "change Date!");
//					Log.d("test", "calendar onPageScrollStateChanged state = "
//							+ Config.contentPagerScrollState);
					if (Config.contentPagerScrollState == 0) {
						changeDate(row3, column3);
					}
				}
			}
			notOutOfRange = true;
			float end = event.getX();
			// android.util.Log.d("touch", "end = " + end);
			// android.util.Log.d("touch", "begin - end = " + (begin - end));
			// if ((end - begin) > SLIDING_LENGTH) {
			// // android.util.Log.d("touch", "toPrevious");
			// if (this.Month == 1) {
			// this.Year--;
			// this.Month = 12;
			// } else {
			// this.Month--;
			// }
			// if (mChangeDateListener != null) {
			// mChangeDateListener.onChangeMonth();
			// }
			// android.util.Log.d("calendar", "Year = " + this.getYear()
			// + ";Month = " + this.getMonth());
			// this.invalidate();
			// }
			// if ((begin - end) > SLIDING_LENGTH) {
			// // android.util.Log.d("touch", "toNext");
			//
			// if (this.Month == 12) {
			// this.Year++;
			// this.Month = 1;
			// } else {
			// this.Month++;
			// }
			// if (mChangeDateListener != null) {
			// mChangeDateListener.onChangeMonth();
			// }
			// android.util.Log.d("calendar", "Year = " + this.getYear()
			// + ";Month = " + this.getMonth());
			// this.invalidate();
			// }
		}
		return true;
	}
	private void changeDate(int row, int column) {
		Cell cell = this.currentMonthArray[row][column];
//		android.util.Log.d("touch", "year = " + cell.year);
//		android.util.Log.d("touch", "month = " + cell.month);
//		android.util.Log.d("touch", "day = " + cell.dayOfMonth);
		setSelectedDay(cell.year, cell.month, cell.dayOfMonth);
		this.invalidate();
	}
	public int getYear() {
		return this.Year;
	}
	public int getMonth() {
		return this.Month;
	}
	public int getDayOfMonth() {
		return this.DayOfMonth;
	}
	public interface ChangeDateListener {
		abstract void onChangeSelectedDate(int year, int month, int day);
		// abstract void onChangeMonth();
	}
	public void setYearAndMonth(int year, int month) {
		this.Year = year;
		this.Month = month;
		this.invalidate();
	}
}
