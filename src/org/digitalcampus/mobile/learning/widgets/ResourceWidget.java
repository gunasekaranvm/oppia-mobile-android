package org.digitalcampus.mobile.learning.widgets;

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.digitalcampus.mobile.learning.R;
import org.digitalcampus.mobile.learning.activity.ModuleActivity;
import org.digitalcampus.mobile.learning.application.MobileLearning;
import org.digitalcampus.mobile.learning.gesture.ResourceGestureDetector;
import org.digitalcampus.mobile.learning.model.Module;
import org.digitalcampus.mquiz.MQuiz;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.bugsense.trace.BugSenseHandler;

public class ResourceWidget extends WidgetFactory {

	public static final String TAG = ResourceWidget.class.getSimpleName();
	private Context ctx;
	private org.digitalcampus.mobile.learning.model.Activity activity;
	private SharedPreferences prefs;
	private long startTimestamp = System.currentTimeMillis()/1000;
	private GestureDetector resourceGestureDetector;
	private OnTouchListener resourceGestureListener; 		
			
	public ResourceWidget(Context context, Module module, org.digitalcampus.mobile.learning.model.Activity activity) {
		super(context, module, activity);
		this.ctx = context;
		this.activity = activity;
		prefs = PreferenceManager.getDefaultSharedPreferences(this.ctx);
		
		resourceGestureDetector = new GestureDetector((Activity) context, new ResourceGestureDetector((ModuleActivity) context));
		resourceGestureListener = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				try {
					// TODO - for some reason unless this is in a try/catch block it will fail with NullPointerException
					return resourceGestureDetector.onTouchEvent(event);
				} catch (Exception e){
					return false;
				}
			}
		};
		
		View vv = super.getLayoutInflater().inflate(R.layout.widget_resource, null);
		LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		super.getLayout().addView(vv,lp);
		vv.setOnTouchListener(resourceGestureListener);
		
		LinearLayout ll = (LinearLayout) ((android.app.Activity) context).findViewById(R.id.widget_resource_object);
		String fileUrl = module.getLocation() + activity.getLocation(prefs.getString(ctx.getString(R.string.prefs_language), Locale.getDefault().getLanguage()));
		
		// show description if any
		String desc = activity.getDescription(prefs.getString(ctx.getString(R.string.prefs_language), Locale.getDefault().getLanguage()));

		TextView descTV = (TextView) ((android.app.Activity) context).findViewById(R.id.widget_resource_description);
		if (desc.length() > 0){
			descTV.setText(desc);
		} else {
			descTV.setVisibility(View.GONE);
		}
		
		// show image files
		if (activity.getMimeType().equals("image/jpeg") || activity.getMimeType().equals("image/png")){
			ImageView iv = new ImageView(this.ctx);
			Bitmap myBitmap = BitmapFactory.decodeFile(fileUrl);
			iv.setImageBitmap(myBitmap);
			ll.addView(iv, lp);
		} else {
		// add button to open other filetypes in whatever app the user has installed as default for that filetype
			Button btn = new Button(this.ctx);
			File file = new File(fileUrl);
			btn.setText(this.ctx.getString(R.string.widget_resource_open_file,file.getName()));
			btn.setTextAppearance(this.ctx, R.style.ButtonText);
			ll.addView(btn);
			
			btn.setTag(file);
			btn.setOnClickListener(new OnClickListener() {
				
				public void onClick(View v) {
					File file = (File) v.getTag();
					// check the file is on the file system (should be but just in case)
					if(!file.exists()){
						Toast.makeText(ResourceWidget.this.ctx, ResourceWidget.this.ctx.getString(R.string.error_resource_not_found,file.getName()), Toast.LENGTH_LONG).show();
						return;
					} 
					Uri targetUri = Uri.fromFile(file);
					
					// check there is actually an app installed to open this filetype
					
					Intent intent = new Intent();
					intent.setAction(android.content.Intent.ACTION_VIEW);
					intent.setDataAndType(targetUri, ResourceWidget.this.activity.getMimeType());
					
					PackageManager pm = ResourceWidget.this.ctx.getPackageManager();

					List<ResolveInfo> infos = pm.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
					boolean appFound = false;
					for (ResolveInfo info : infos) {
						IntentFilter filter = info.filter;
						if (filter != null && filter.hasAction(Intent.ACTION_VIEW)) {
							// Found an app with the right intent/filter
							appFound = true;
						}
					}

					if(appFound){
						ResourceWidget.this.ctx.startActivity(intent);
					} else {
						Toast.makeText(ResourceWidget.this.ctx, ResourceWidget.this.ctx.getString(R.string.error_resource_app_not_found,file.getName()), Toast.LENGTH_LONG).show();
					}
				}
			});
		}
	}

	@Override
	public boolean activityHasTracker() {
		long endTimestamp = System.currentTimeMillis()/1000;
		long diff = endTimestamp - startTimestamp;
		if(diff >= MobileLearning.PAGE_READ_TIME){
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean activityCompleted() {
		return true;
	}

	@Override
	public long getTimeTaken() {
		long endTimestamp = System.currentTimeMillis()/1000;
		long diff = endTimestamp - startTimestamp;
		if(diff >= MobileLearning.PAGE_READ_TIME){
			return diff;
		} else {
			return 0;
		}
	}

	@Override
	public void setStartTime(long startTime) {
		this.startTimestamp = startTime;
	}

	@Override
	public long getStartTime() {
		return this.startTimestamp;
	}

	@Override
	public JSONObject getTrackerData() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("timetaken", this.getTimeTaken());
			String lang = prefs.getString(ctx.getString(R.string.prefs_language), Locale.getDefault().getLanguage());
			obj.put("lang", lang);
		} catch (JSONException e) {
			e.printStackTrace();
			BugSenseHandler.sendException(e);
		}
		
		return obj;
	}

	@Override
	public String getContentToRead() {
		return "";
	}

	@Override
	public String getMediaFileName() {
		return null;
	}

	@Override
	public void setMediaFileName(String mediaFileName) {
		
	}

	@Override
	public void mediaStopped() {
		
	}

	@Override
	public boolean getMediaPlaying() {
		return false;
	}

	@Override
	public long getMediaStartTime() {
		return 0;
	}

	@Override
	public void setMediaPlaying(boolean playing) {
		
	}

	@Override
	public void setMediaStartTime(long mediaStartTime) {
		
	}

	@Override
	public void setReadAloud(boolean reading) {

		
	}

	@Override
	public boolean getReadAloud() {
		return false;
	}

	@Override
	public MQuiz getMQuiz() {
		return null;
	}

	@Override
	public void setMQuiz(MQuiz mquiz) {
		
	}

}
