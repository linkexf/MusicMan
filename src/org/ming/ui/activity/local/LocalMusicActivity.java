package org.ming.ui.activity.local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ming.R;
import org.ming.center.Controller;
import org.ming.center.GlobalSettingParameter;
import org.ming.center.MobileMusicApplication;
import org.ming.center.database.DBController;
import org.ming.center.database.Playlist;
import org.ming.center.system.SystemEventListener;
import org.ming.center.ui.UIGlobalSettingParameter;
import org.ming.ui.util.DialogUtil;
import org.ming.util.MyLogger;
import org.ming.util.Util;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.text.method.TextKeyListener;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class LocalMusicActivity extends ListActivity implements
		SystemEventListener {
	private static final MyLogger logger = MyLogger
			.getLogger("LocalMusicActivity");
	private static final int LIST_ITEM_ID_ALL_SONG = 0; // 所有的音乐
	private static final int LIST_ITEM_ID_BROWSE_BY_CATALOG = 2; // 分类音乐
	private static final int LIST_ITEM_ID_BROWSE_BY_SINGER = 1; // 歌手分类
	private static final int LIST_ITEM_ID_DOWNLOAD_DOLBY_MUSIC = 4; // 杜比音乐
	private static final int LIST_ITEM_ID_DWONLOAD_MUSIC = 3; // 已下载的音乐
	private final int CONTEXT_MENU_DELETE = 0; // 删除
	private final int CONTEXT_MENU_RENAME = 1; // 重命名
	private final int MENU_ITEM_EXIT = 1; // 退出
	private final int MENU_ITEM_SCAN_MUSIC = 0; // 扫描音乐
	private final int MENU_ITEM_SET = 2; // 设置
	private final int MENU_ITEM_TIME_CLOSE = 3; // 定时关闭应用
	Intent intent = null;
	long itemId;
	private SimpleAdapter mAdapter = null;
	private int mAllSongsNumber = 0;
	private Button mBtnCreatePlayList;
	private Controller mController = null;
	private int mCurLongPressSelectedItem = -1;
	private Dialog mCurrentDialog = null;
	private DBController mDBController = null;
	private int mDownloadDoblyFileNumber = 0;
	private int mDownloadFileNumber = 0;
	private int mFolderNumber = 0;
	private Button mSearchBtn;
	private int mSingerNumber = 0;
	private List<Playlist> mLocalPlayList = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		logger.v("onCreate() ---> Enter");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_local_music_layout);
		logger.v("--------------------");
		this.mController = ((MobileMusicApplication) getApplication()).getController();
		this.mDBController = this.mController.getDBController();
		UIGlobalSettingParameter.localmusic_scan_smallfile = this.mDBController
				.getScanSmallSongFile();
		if (UIGlobalSettingParameter.localmusic_folder_names == null) {
			String str = this.mDBController.getLocalFolder();
			if (str != null) {
				UIGlobalSettingParameter.localmusic_folder_names = str
						.split(";");
			}
		}

		View localView = getLayoutInflater().inflate(
				R.layout.activity_local_music_layout_header, null);
		this.mSearchBtn = (Button) localView
				.findViewById(R.id.search_local_music);
		this.mSearchBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!Environment.MEDIA_MOUNTED.equals(Environment
						.getExternalStorageState())) {
					Toast.makeText(LocalMusicActivity.this,
							R.string.sdcard_missing_message_common,
							Toast.LENGTH_SHORT).show();
				} else {
					Intent localIntent = new Intent(LocalMusicActivity.this,
							LocalMusicSearchActivity.class);
					LocalMusicActivity.this.startActivity(localIntent);
				}
			}
		});

		getListView().addHeaderView(localView);
		this.mBtnCreatePlayList = (Button) findViewById(R.id.button_create_playlist);
		getListView().setOnCreateContextMenuListener(
				new View.OnCreateContextMenuListener() {

					@Override
					public void onCreateContextMenu(ContextMenu menu, View v,
							ContextMenuInfo menuInfo) {
						AdapterContextMenuInfo localAdapterContextMenuInfo = (AdapterContextMenuInfo) menuInfo;
						if ((int) localAdapterContextMenuInfo.id > 4) {
							LocalMusicActivity.this.mCurLongPressSelectedItem = ((int) localAdapterContextMenuInfo.id) - 4;
							menu.add(0, 0, 0,
									R.string.local_music_delete_songlist);
							menu.add(1, 1, 0,
									R.string.local_music_rename_songlist);
						}
					}
				});

		this.mBtnCreatePlayList
				.setOnClickListener(this.mCreatePlayListOnClickListener);
		this.mController.addSystemEventListener(22, this);
		logger.v("onCreate() ---> Exit");
	}

	private View.OnClickListener mCreatePlayListOnClickListener = new View.OnClickListener() {
		public void onClick(View paramAnonymousView) {
			LocalMusicActivity.logger.v("mLoadMoreOnClickListener() ---> Exit");
			LocalMusicActivity.this.CreatePlayList();
			LocalMusicActivity.logger.v("mLoadMoreOnClickListener() ---> Exit");
		}
	};

	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem localMenuItem = menu.findItem(0);
		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {
			localMenuItem.setEnabled(true);
		} else {
			localMenuItem.setEnabled(false);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "").setIcon(R.drawable.menu_item_scan_selector);
		menu.add(2, 2, 0, "").setIcon(R.drawable.menu_item_set_selector);
		menu.add(3, 3, 0, "").setIcon(R.drawable.menu_item_time_close_selector);
		menu.add(1, 1, 0, "").setIcon(R.drawable.menu_item_exit_selector);
		return super.onCreateOptionsMenu(menu);
	}

	protected void onResume() {
		logger.v("onResume() ----> Enter");
		super.onResume();
		if ((UIGlobalSettingParameter.localmusic_folder_names == null)
				&& (UIGlobalSettingParameter.localmusic_scan_warningdlg)
				&& (Environment.MEDIA_MOUNTED.equals(Environment
						.getExternalStorageState()))) {
			if (this.mCurrentDialog != null) {
				this.mCurrentDialog.dismiss();
				this.mCurrentDialog = null;
			}
			UIGlobalSettingParameter.localmusic_scan_warningdlg = false;
			this.mCurrentDialog = DialogUtil.show2BtnDialogWithIconTitleMsg(
					this, getText(R.string.title_information_common),
					getText(R.string.local_music_add_common),
					new View.OnClickListener() {
						public void onClick(View paramAnonymousView) {
							Intent localIntent = new Intent(
									LocalMusicActivity.this,
									LocalScanMusicActivity.class);
							LocalMusicActivity.this.startActivity(localIntent);
							UIGlobalSettingParameter.SHOW_SCAN_CONSEQUENSE = true;
							if (LocalMusicActivity.this.mCurrentDialog != null) {
								LocalMusicActivity.this.mCurrentDialog
										.dismiss();
								LocalMusicActivity.this.mCurrentDialog = null;
							}
						}
					}, new View.OnClickListener() {
						public void onClick(View paramAnonymousView) {
							if (LocalMusicActivity.this.mCurrentDialog != null) {
								LocalMusicActivity.this.mCurrentDialog
										.dismiss();
								LocalMusicActivity.this.mCurrentDialog = null;
							}
						}
					});
		} else if (UIGlobalSettingParameter.localmusic_folder_names != null) {
			this.mAllSongsNumber = this.mDBController.getAllSongsCountByFolder(
					UIGlobalSettingParameter.localmusic_folder_names,
					UIGlobalSettingParameter.localmusic_scan_smallfile);
			this.mSingerNumber = this.mDBController.getArtistCountByFolder(
					UIGlobalSettingParameter.localmusic_folder_names,
					UIGlobalSettingParameter.localmusic_scan_smallfile);
			if (!Environment.MEDIA_MOUNTED.equals(Environment
					.getExternalStorageState())) {
				Toast.makeText(LocalMusicActivity.this,
						R.string.sdcard_missing_message_common,
						Toast.LENGTH_SHORT).show();
			}

			for (this.mFolderNumber = UIGlobalSettingParameter.localmusic_folder_names.length;; this.mFolderNumber = 0) {
				if (UIGlobalSettingParameter.SHOW_SCAN_CONSEQUENSE) {
					Object[] arrayOfObject = new Object[1];
					arrayOfObject[0] = Integer.valueOf(this.mAllSongsNumber);
					Toast.makeText(
							this,
							getString(R.string.local_music_after_scan,
									arrayOfObject), 1).show();
					UIGlobalSettingParameter.SHOW_SCAN_CONSEQUENSE = false;
				}
				DBController localDBController1 = this.mDBController;
				String[] arrayOfString1 = new String[1];
				arrayOfString1[0] = GlobalSettingParameter.LOCAL_PARAM_MUSIC_STORE_SD_DIR;
				this.mDownloadFileNumber = localDBController1
						.getAllSongsCountByFolder(
								arrayOfString1,
								UIGlobalSettingParameter.localmusic_scan_smallfile);
				DBController localDBController2 = this.mDBController;
				String[] arrayOfString2 = new String[1];
				arrayOfString2[0] = GlobalSettingParameter.LOCAL_PARAM_DOBLY_MUSIC_STORE_SD_DIR;
				this.mDownloadDoblyFileNumber = localDBController2
						.getAllSongsCountByFolder(
								arrayOfString2,
								UIGlobalSettingParameter.localmusic_scan_smallfile);
				refreshUI();

				logger.v("onResume() ---> Exit");
			}
		} else if (!Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {
			Toast.makeText(LocalMusicActivity.this,
					R.string.sdcard_missing_message_common, Toast.LENGTH_SHORT)
					.show();
		}
	}

	protected void onDestroy() {
		logger.v("onDestroy() ---> Enter");
		this.mController.removeSystemEventListener(22, this);
		super.onDestroy();
		logger.v("onDestroy() ---> Exit");
	}

	private void refreshUI() {
		ArrayList localArrayList1 = new ArrayList(4);
		String[] arrayOfString = { "title", "num", "icon" };
		int[] arrayOfInt = { R.id.text1, R.id.text2, R.id.listicon1 };
		// 全部歌曲
		Object[] arrayOfObject1 = new Object[1];
		arrayOfObject1[0] = Integer.valueOf(this.mAllSongsNumber);
		addRow(localArrayList1, getText(R.string.local_music_all_song)
				.toString(),
				getString(R.string.local_music_all_song_num, arrayOfObject1)
						.toString(), R.drawable.list_cell_button_arrow_selector);
		// 按歌手浏览
		Object[] arrayOfObject2 = new Object[1];
		arrayOfObject2[0] = Integer.valueOf(this.mSingerNumber);
		addRow(localArrayList1,
				getText(R.string.local_music_browse_by_singer).toString(),
				getString(R.string.local_music_browse_by_singer_num,
						arrayOfObject2).toString(),
				R.drawable.list_cell_button_arrow_selector);
		// 按目录浏览
		addRow(localArrayList1, getText(R.string.local_music_browse_by_catalog)
				.toString(), null, R.drawable.list_cell_button_arrow_selector);

		// 已付费下载
		Object[] arrayOfObject3 = new Object[1];
		arrayOfObject3[0] = Integer.valueOf(this.mDownloadFileNumber);
		addRow(localArrayList1, getText(R.string.local_music_download_music)
				.toString(),
				getString(R.string.local_music_all_song_num, arrayOfObject3)
						.toString(), R.drawable.list_cell_button_arrow_selector);
		// 杜比音乐
		Object[] arrayOfObject4 = new Object[1];
		arrayOfObject4[0] = Integer.valueOf(this.mDownloadDoblyFileNumber);
		addRow(localArrayList1, getText(R.string.dobly_song_number).toString(),
				getString(R.string.local_music_all_song_num, arrayOfObject4)
						.toString(), R.drawable.list_cell_button_arrow_selector);

		this.mLocalPlayList = this.mDBController.getAllPlaylists(1);
		Object localObject = null;

		for (Iterator localIterator = this.mLocalPlayList.iterator(); localIterator
				.hasNext();) {
			Playlist localPlaylist = (Playlist) localIterator.next();
			if (!localPlaylist.mName
					.equals("cmccwm.mobilemusic.database.default.local.playlist.recent.download")) {
				ArrayList localArrayList2 = (ArrayList) this.mDBController
						.getSongsFromPlaylist(localPlaylist.mExternalId, 1);
				String str5 = localPlaylist.mName;
				Object[] arrayOfObject5 = new Object[1];
				if (localArrayList2 == null)
					;
				for (int i = 0;; i = localArrayList2.size()) {
					arrayOfObject5[0] = Integer.valueOf(i);
					addRow(localArrayList1,
							str5,
							getString(R.string.local_music_all_song_num,
									arrayOfObject5).toString(),
							R.drawable.list_cell_button_arrow_selector);
				}
			}
			localObject = localPlaylist;
		}
		if (localObject != null)
			this.mLocalPlayList.remove(localObject);
		this.mAdapter = new SimpleAdapter(this, localArrayList1,
				R.layout.cmcc_list_2, arrayOfString, arrayOfInt);
		setListAdapter(this.mAdapter);
	}

	private void addRow(List<Map<String, Object>> list, String string1,
			String string2, int paramInt) {
		HashMap localHashMap = new HashMap();
		localHashMap.put("title", string1);
		localHashMap.put("num", string2);
		localHashMap.put("icon", Integer.valueOf(paramInt));
		list.add(localHashMap);
	}

	private void CreatePlayList() {
		View localView = getLayoutInflater().inflate(
				R.layout.activity_my_migu_music_create_playlist_layout, null);
		final EditText localEditText = (EditText) localView
				.findViewById(R.id.new_playlist);
		localEditText.setKeyListener(TextKeyListener.getInstance());
		localEditText.selectAll();
		((ListView) findViewById(android.R.id.list))
				.setVerticalScrollBarEnabled(false);
		this.mCurrentDialog = DialogUtil.show2BtnDialogWithIconTitleView(
				getParent(),
				getText(R.string.create_play_list_playlist_activity), null,
				localView, new View.OnClickListener() {
					public void onClick(View paramAnonymousView) {
						String str = localEditText.getText().toString();
						((ListView) LocalMusicActivity.this
								.findViewById(android.R.id.list))
								.setVerticalScrollBarEnabled(true);
						if ((str == null) || ("".equals(str.trim())))
							Toast.makeText(
									LocalMusicActivity.this,
									R.string.invalid_playlist_name_playlist_activity,
									1).show();
						if ((str.equals(LocalMusicActivity.this
								.getString(R.string.playlist_myfav_common)))
								|| (str.equals(LocalMusicActivity.this
										.getString(R.string.playlist_recent_play_common)))
								|| (str.equals(LocalMusicActivity.this
										.getString(R.string.local_music_all_song)))
								|| (str.equals(LocalMusicActivity.this
										.getString(R.string.local_music_browse_by_singer)))
								|| (str.equals(LocalMusicActivity.this
										.getString(R.string.local_music_browse_by_catalog)))
								|| (str.equals(LocalMusicActivity.this
										.getString(R.string.local_music_download_music)))
								|| (str.equals(LocalMusicActivity.this
										.getString(R.string.dobly_song_number)))) {
							Toast.makeText(
									LocalMusicActivity.this,
									R.string.duplicate_playlist_edit_playlist_activity,
									1).show();
						} else if (LocalMusicActivity.this.mDBController
								.getPlaylistByName(str, 1) != null) {
							Toast.makeText(
									LocalMusicActivity.this,
									R.string.duplicate_playlist_edit_playlist_activity,
									1).show();
						} else {
							LocalMusicActivity.this.mDBController
									.createPlaylist(str, 1);
							LocalMusicActivity.this.refreshUI();
							if (LocalMusicActivity.this.mCurrentDialog != null) {
								LocalMusicActivity.this.mCurrentDialog
										.dismiss();
								LocalMusicActivity.this.mCurrentDialog = null;
							}
							Toast.makeText(
									LocalMusicActivity.this,
									LocalMusicActivity.this
											.getString(
													R.string.create_playlist_successfully_edit_playlist_activity,
													new Object[] { str }), 1)
									.show();
						}
					}
				}, new View.OnClickListener() {
					public void onClick(View paramAnonymousView) {
						((ListView) LocalMusicActivity.this
								.findViewById(android.R.id.list))
								.setVerticalScrollBarEnabled(true);
						if (LocalMusicActivity.this.mCurrentDialog != null) {
							LocalMusicActivity.this.mCurrentDialog.dismiss();
							LocalMusicActivity.this.mCurrentDialog = null;
						}
					}
				});
	}

	@Override
	public void handleSystemEvent(Message paramMessage) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem paramMenuItem) {
		boolean bool = true;
		switch (paramMenuItem.getItemId()) {
		default:
			bool = super.onOptionsItemSelected(paramMenuItem);
		case 0:
			startActivity(new Intent(this, LocalScanMusicActivity.class));
			break;
		case 1:
			// startActivity(new Intent(this, MobileMusicMoreActivity.class));
			break;
		case 2:
			// startActivity(new Intent(this, TimingClosureActivity.class));
			break;
		case 3:
			exitApplication();
			break;
		}

		UIGlobalSettingParameter.SHOW_SCAN_CONSEQUENSE = bool;

		return bool;
	}

	private void exitApplication() {
		this.mCurrentDialog = DialogUtil.show2BtnDialogWithIconTitleMsg(this,
				getText(R.string.quit_app_dialog_title),
				getText(R.string.quit_app_dialog_message),
				new View.OnClickListener() {
					public void onClick(View paramAnonymousView) {
						if (LocalMusicActivity.this.mCurrentDialog != null) {
							LocalMusicActivity.this.mCurrentDialog.dismiss();
							LocalMusicActivity.this.mCurrentDialog = null;
						}
						Util.exitMobileMusicApp(false);
					}
				}, new View.OnClickListener() {
					public void onClick(View paramAnonymousView) {
						if (LocalMusicActivity.this.mCurrentDialog != null) {
							LocalMusicActivity.this.mCurrentDialog.dismiss();
							LocalMusicActivity.this.mCurrentDialog = null;
						}
					}
				});
		this.mCurrentDialog.setCancelable(false);
	}
}