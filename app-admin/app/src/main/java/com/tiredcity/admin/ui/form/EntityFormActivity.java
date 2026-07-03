package com.tiredcity.admin.ui.form;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tiredcity.admin.R;
import com.tiredcity.admin.data.AdminModule;
import com.tiredcity.admin.data.ModuleForm;
import com.tiredcity.admin.data.model.FormField;
import com.tiredcity.admin.databinding.ActivityEntityFormBinding;
import com.tiredcity.admin.utils.AuditLogger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Man hinh them / sua 1 ban ghi cho cac module co CRUD (mirror form ben web-admin).
 * Truong nhap lieu duoc dung dong tu {@link ModuleForm#fields}; luu truc tiep vao Firestore.
 */
public class EntityFormActivity extends AppCompatActivity {

    public static final String EXTRA_MODULE = "extra_module";
    /** null / khong co -> them moi; nguoc lai la id document can sua. */
    public static final String EXTRA_DOC_ID = "extra_doc_id";

    private interface ValueReader {
        String read();
    }

    private static final class Binding {
        final FormField field;
        final ValueReader reader;
        final EditText errorTarget; // co the null (Spinner/Switch)

        Binding(FormField field, ValueReader reader, EditText errorTarget) {
            this.field = field;
            this.reader = reader;
            this.errorTarget = errorTarget;
        }
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ActivityEntityFormBinding binding;

    private AdminModule module;
    private String docId;
    private boolean isCreate;
    private boolean saving;

    private final List<Binding> bindings = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEntityFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            module = AdminModule.valueOf(getIntent().getStringExtra(EXTRA_MODULE));
        } catch (Exception e) {
            finish();
            return;
        }
        if (module.collection == null) {
            finish();
            return;
        }

        docId = getIntent().getStringExtra(EXTRA_DOC_ID);
        isCreate = docId == null || docId.isEmpty();

        int color = ContextCompat.getColor(this, module.colorRes);
        binding.headerBar.setBackgroundColor(color);
        getWindow().setStatusBarColor(color);
        binding.btnSaveBottom.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        binding.tvTitle.setText(getString(
                isCreate ? R.string.form_create_fmt : R.string.form_edit_fmt,
                getString(module.title)));

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> save());
        binding.btnSaveBottom.setOnClickListener(v -> save());

        if (isCreate) {
            renderFields(ModuleForm.fields(module, null));
        } else {
            loadForEdit();
        }
    }

    private void loadForEdit() {
        db.collection(module.collection).document(docId).get()
                .addOnSuccessListener(d -> {
                    if (!d.exists()) {
                        Toast.makeText(this, R.string.record_not_found, Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    renderFields(ModuleForm.fields(module, d));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.load_error, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    // ------------------------------------------------------------------ rendering

    private void renderFields(List<FormField> fields) {
        LinearLayout container = binding.containerFields;
        container.removeAllViews();
        bindings.clear();

        for (int i = 0; i < fields.size(); i++) {
            FormField f = fields.get(i);
            if (f.type == FormField.Type.SWITCH) {
                addSwitch(container, f, i > 0);
            } else {
                addLabel(container, f, i > 0);
                switch (f.type) {
                    case SELECT:
                        addSpinner(container, f);
                        break;
                    case DATE:
                        addDate(container, f);
                        break;
                    default:
                        addEditText(container, f);
                        break;
                }
            }
        }
    }

    private void addLabel(LinearLayout parent, FormField f, boolean spaced) {
        TextView tv = new TextView(this);
        tv.setText(f.required ? f.label + " *" : f.label);
        tv.setTextColor(ContextCompat.getColor(this, R.color.tc_text_secondary));
        tv.setTextSize(13f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(spaced ? 14 : 2);
        lp.bottomMargin = dp(6);
        tv.setLayoutParams(lp);
        parent.addView(tv);
    }

    private void addEditText(LinearLayout parent, FormField f) {
        EditText et = new EditText(this);
        et.setBackgroundResource(R.drawable.bg_input);
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        et.setTextColor(ContextCompat.getColor(this, R.color.tc_text_primary));
        et.setTextSize(15f);
        et.setText(f.value);
        if (f.hint != null) et.setHint(f.hint);
        switch (f.type) {
            case TEXTAREA:
                et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                et.setMinLines(3);
                et.setGravity(Gravity.TOP | Gravity.START);
                break;
            case NUMBER:
                et.setInputType(InputType.TYPE_CLASS_NUMBER);
                et.setSingleLine(true);
                break;
            case DECIMAL:
                et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                et.setSingleLine(true);
                break;
            default:
                et.setInputType(InputType.TYPE_CLASS_TEXT);
                et.setSingleLine(true);
                break;
        }
        et.setLayoutParams(fieldParams());
        parent.addView(et);
        bindings.add(new Binding(f, () -> et.getText().toString().trim(), et));
    }

    private void addDate(LinearLayout parent, FormField f) {
        EditText et = new EditText(this);
        et.setBackgroundResource(R.drawable.bg_input);
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        et.setTextColor(ContextCompat.getColor(this, R.color.tc_text_primary));
        et.setTextSize(15f);
        et.setText(f.value);
        et.setHint(R.string.hint_pick_date);
        et.setFocusable(false);
        et.setClickable(true);
        et.setInputType(InputType.TYPE_NULL);
        et.setLayoutParams(fieldParams());
        et.setOnClickListener(v -> pickDate(et));
        parent.addView(et);
        bindings.add(new Binding(f, () -> et.getText().toString().trim(), et));
    }

    private void pickDate(EditText target) {
        Calendar c = Calendar.getInstance();
        String cur = target.getText().toString().trim();
        if (cur.length() >= 10 && cur.charAt(4) == '-') {
            try {
                String[] p = cur.substring(0, 10).split("-");
                c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]));
            } catch (Exception ignored) {}
        }
        new DatePickerDialog(this, (view, year, month, day) ->
                target.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void addSpinner(LinearLayout parent, FormField f) {
        Spinner sp = new Spinner(this);
        sp.setBackgroundResource(R.drawable.bg_input);
        sp.setPadding(dp(10), dp(6), dp(10), dp(6));
        ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, f.options);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        int sel = 0;
        for (int i = 0; i < f.optionValues.length; i++) {
            if (f.optionValues[i].equals(f.value)) {
                sel = i;
                break;
            }
        }
        sp.setSelection(sel);
        sp.setLayoutParams(fieldParams());
        parent.addView(sp);
        bindings.add(new Binding(f, () -> {
            int pos = sp.getSelectedItemPosition();
            return pos >= 0 && pos < f.optionValues.length ? f.optionValues[pos] : "";
        }, null));
    }

    private void addSwitch(LinearLayout parent, FormField f, boolean spaced) {
        SwitchCompat sw = new SwitchCompat(this);
        sw.setText(f.label);
        sw.setTextColor(ContextCompat.getColor(this, R.color.tc_text_primary));
        sw.setTextSize(15f);
        sw.setChecked("true".equals(f.value));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(spaced ? 18 : 4);
        sw.setLayoutParams(lp);
        parent.addView(sw);
        bindings.add(new Binding(f, () -> sw.isChecked() ? "true" : "false", null));
    }

    private LinearLayout.LayoutParams fieldParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    // ------------------------------------------------------------------ save

    private void save() {
        if (saving) return;

        Map<String, String> values = new HashMap<>();
        Binding firstInvalid = null;
        for (Binding b : bindings) {
            String v = b.reader.read();
            values.put(b.field.key, v);
            if (b.field.required && (v == null || v.isEmpty())) {
                if (firstInvalid == null) firstInvalid = b;
                if (b.errorTarget != null) b.errorTarget.setError(getString(R.string.field_required));
            }
        }
        if (firstInvalid != null) {
            if (firstInvalid.errorTarget != null) firstInvalid.errorTarget.requestFocus();
            Toast.makeText(this, R.string.form_fill_required, Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> payload = ModuleForm.payload(module, values, isCreate);
        setSaving(true);

        if (isCreate) {
            db.collection(module.collection).add(payload)
                    .addOnSuccessListener(ref -> {
                        AuditLogger.log(ModuleForm.auditCreate(module),
                                ModuleForm.targetOf(module, values), "");
                        Toast.makeText(this, R.string.saved_ok, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(this::onSaveError);
        } else {
            db.collection(module.collection).document(docId).update(payload)
                    .addOnSuccessListener(x -> {
                        AuditLogger.log(ModuleForm.auditUpdate(module),
                                ModuleForm.targetOf(module, values), "");
                        Toast.makeText(this, R.string.saved_ok, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(this::onSaveError);
        }
    }

    private void onSaveError(Exception e) {
        setSaving(false);
        Toast.makeText(this, getString(R.string.update_error, e.getMessage()),
                Toast.LENGTH_LONG).show();
    }

    private void setSaving(boolean value) {
        saving = value;
        binding.btnSave.setEnabled(!value);
        binding.btnSaveBottom.setEnabled(!value);
        binding.btnSaveBottom.setText(value ? R.string.saving : R.string.btn_save);
    }
}
