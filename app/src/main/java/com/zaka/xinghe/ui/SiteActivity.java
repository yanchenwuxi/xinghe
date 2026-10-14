package com.zaka.xinghe.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.zaka.xinghe.App;
import com.zaka.xinghe.R;
import com.zaka.xinghe.adapter.SiteAdapter;
import com.zaka.xinghe.model.Site;
import com.zaka.xinghe.net.CmsApi;
import com.zaka.xinghe.store.SiteStore;
import com.zaka.xinghe.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * 接口管理页。
 * 配置弹窗对齐影视仓：名称 + 地址 + 选择本地文件。
 */
public class SiteActivity extends Activity {

    private static final int REQ_PICK_FILE = 1001;

    private EditText etUrl;
    private ListView list;
    private TextView emptyText;
    private SiteAdapter adapter;

    // 弹窗内控件（选择文件后回填）
    private EditText dialogEtUrl;
    private EditText dialogEtName;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_site);
        etUrl = (EditText) findViewById(R.id.etUrl);
        list = (ListView) findViewById(R.id.list);
        emptyText = (TextView) findViewById(R.id.emptyText);
        adapter = new SiteAdapter(this);
        list.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.btnAdd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String u = etUrl.getText().toString().trim();
                if (!Utils.empty(u)) {
                    doImport(u, null);
                } else {
                    showSourceDialog(null);
                }
            }
        });
        findViewById(R.id.btnLocalFile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSourceDialog(etUrl.getText().toString().trim());
            }
        });
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switchTo(adapter.data().get(position));
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                final Site s = adapter.data().get(position);
                String typeHint = s.isSpiderOrScript() ? "（脚本/爬虫源）" : "";
                new AlertDialog.Builder(SiteActivity.this)
                        .setTitle(s.name + typeHint)
                        .setItems(new String[]{"删除这个接口", "查看详情"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                if (which == 0) {
                                    SiteStore.remove(s);
                                    refresh();
                                } else {
                                    showSiteInfo(s);
                                }
                            }
                        })
                        .show();
                return true;
            }
        });
        refresh();
    }

    /** 对齐影视仓的配置地址弹窗。 */
    private void showSourceDialog(String presetUrl) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_source_input);
        dialog.setCancelable(true);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialogEtName = (EditText) dialog.findViewById(R.id.etName);
        dialogEtUrl = (EditText) dialog.findViewById(R.id.etUrl);
        if (!Utils.empty(presetUrl)) {
            dialogEtUrl.setText(presetUrl);
        }

        dialog.findViewById(R.id.btnPickFile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickLocalFile();
            }
        });
        dialog.findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.btnOk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = dialogEtUrl.getText().toString().trim();
                String name = dialogEtName.getText().toString().trim();
                if (Utils.empty(url)) {
                    Utils.toast("请输入接口地址或选择本地文件");
                    return;
                }
                dialog.dismiss();
                doImport(url, name);
            }
        });
        dialog.show();
    }

    private void pickLocalFile() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] mime = new String[]{"application/json", "text/plain", "text/*", "application/octet-stream"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mime);
            startActivityForResult(intent, REQ_PICK_FILE);
        } catch (Throwable t) {
            // 兼容旧机
            try {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, "选择接口配置文件"), REQ_PICK_FILE);
            } catch (Throwable t2) {
                Utils.toast("无法打开文件选择器，请手动输入路径");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_FILE || resultCode != RESULT_OK || data == null) return;
        final Uri uri = data.getData();
        if (uri == null) return;
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Throwable ignore) {
        }
        Utils.toast("正在读取文件…");
        App.run(new App.Task() {
            @Override
            public Object call() throws Exception {
                return readUriContent(uri);
            }
        }, new App.Done() {
            @Override
            public void onDone(Object result, Exception err) {
                if (err != null || result == null) {
                    Utils.toast("读取文件失败");
                    return;
                }
                String content = String.valueOf(result);
                String displayName = queryDisplayName(uri);
                if (dialogEtUrl != null) {
                    // 弹窗还在：把内容填进地址框（或直接导入）
                    dialogEtUrl.setText(content.length() > 2000 ? content.substring(0, 2000) + "…" : content);
                    // 内容太长时直接导入更合适
                    if (content.length() > 500) {
                        if (dialogEtName != null && Utils.empty(dialogEtName.getText().toString())
                                && !Utils.empty(displayName)) {
                            dialogEtName.setText(displayName.replace(".json", "").replace(".txt", ""));
                        }
                        doImport(content, dialogEtName != null ? dialogEtName.getText().toString().trim() : displayName);
                    }
                } else {
                    doImport(content, displayName);
                }
            }
        });
    }

    private String readUriContent(Uri uri) throws Exception {
        InputStream in = getContentResolver().openInputStream(uri);
        if (in == null) throw new RuntimeException("无法打开文件");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        in.close();
        return new String(bos.toByteArray(), "UTF-8");
    }

    private String queryDisplayName(Uri uri) {
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return c.getString(idx);
            }
        } catch (Throwable ignore) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignore) {}
        }
        return "本地配置";
    }

    private void doImport(final String text, final String preferName) {
        Utils.toast("正在导入…");
        App.run(new App.Task() {
            @Override
            public Object call() throws Exception {
                String msg = SiteStore.importConfig(text);
                if (!Utils.empty(preferName)) {
                    // 若只导入了一个站点且有自定义名，覆盖名称
                    List<Site> all = SiteStore.sites();
                    if (all.size() > 0) {
                        Site last = all.get(all.size() - 1);
                        // 仅当名称像自动生成时才改
                        if (last.name.startsWith("站点") || last.name.contains(".") || last.name.equals(preferName)) {
                            last.name = preferName;
                            SiteStore.add(last);
                        }
                    }
                }
                return msg;
            }
        }, new App.Done() {
            @Override
            public void onDone(Object result, Exception err) {
                if (err != null) {
                    Utils.toast("导入失败：" + (err.getMessage() != null ? err.getMessage() : "解析错误"));
                    return;
                }
                Utils.toast(String.valueOf(result));
                if (etUrl != null) etUrl.setText("");
                refresh();
            }
        });
    }

    private void showSiteInfo(Site s) {
        StringBuilder sb = new StringBuilder();
        sb.append("名称：").append(s.name).append("\n");
        sb.append("类型：").append(s.type).append(" (code=").append(s.typeCode).append(")\n");
        sb.append("地址：").append(s.api).append("\n");
        if (!Utils.empty(s.ext)) sb.append("扩展：").append(s.ext).append("\n");
        if (!Utils.empty(s.jar)) sb.append("Jar：").append(s.jar).append("\n");
        if (s.isSpiderOrScript()) {
            sb.append("\n脚本/爬虫源将尝试：\n");
            sb.append("• Jar → DexClassLoader 加载\n");
            sb.append("• JS → 系统 WebView 引擎（可扩展 QuickJS）\n");
            sb.append("• Python → 需集成 Chaquopy");
        }
        new AlertDialog.Builder(this)
                .setTitle("接口详情")
                .setMessage(sb.toString())
                .setPositiveButton("确定", null)
                .show();
    }

    private void refresh() {
        List<Site> data = SiteStore.sites();
        adapter.setData(data);
        emptyText.setVisibility(data.size() == 0 ? View.VISIBLE : View.GONE);
    }

    private void switchTo(final Site s) {
        doSwitch(s);
    }

    private void doSwitch(final Site s) {
        SiteStore.setCurrent(s);
        Utils.toast("已切换到 " + s.name + "，正在重新加载");
        App.run(new App.Task() {
            @Override
            public Object call() throws Exception {
                if (s.isSpiderOrScript()) {
                    // 预热 Spider
                    try {
                        com.zaka.xinghe.spider.SpiderFactory.create(SiteActivity.this, s);
                    } catch (Throwable t) {
                        // 仍允许切换，使用时再报错
                        return "spider:" + t.getMessage();
                    }
                    return "spider_ok";
                }
                return CmsApi.getCates(s);
            }
        }, new App.Done() {
            @Override
            public void onDone(Object result, Exception err) {
                refresh();
                if (result instanceof String && String.valueOf(result).startsWith("spider:")) {
                    Utils.toast("脚本源加载提示：" + String.valueOf(result).substring(7));
                    return;
                }
                if (err != null) {
                    Utils.toast("加载失败：" + err.getMessage());
                    return;
                }
                if (result instanceof List) {
                    int n = ((List) result).size();
                    if (n == 0) Utils.toast("这个接口没解析出分类，可能不支持或已失效");
                    else Utils.toast("接口正常，共 " + n + " 个分类");
                } else {
                    Utils.toast("已切换");
                }
            }
        });
    }
}
