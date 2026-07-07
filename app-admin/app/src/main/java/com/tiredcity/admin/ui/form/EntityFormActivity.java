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
    private android.text.TextWatcher activeValueWatcher;

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
                    case TIME:
                        addTime(container, f);
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
        et.setTextColor(ContextCompat.getColor(this, f.enabled ? R.color.tc_text_primary : R.color.tc_text_secondary));
        et.setTextSize(15f);
        et.setText(f.value);
        et.setEnabled(f.enabled);
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
            case MONEY:
                et.setInputType(InputType.TYPE_CLASS_NUMBER);
                et.setSingleLine(true);
                setupMoneyInput(et);
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
        et.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                et.setError(null);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        parent.addView(et);
        bindings.add(new Binding(f, () -> {
            String val = et.getText().toString().trim();
            if (f.type == FormField.Type.MONEY || (module == AdminModule.VOUCHERS && "value".equals(f.key))) {
                return val.replaceAll("[^0-9]", "");
            }
            return val;
        }, et));
    }

    private void updateVoucherValueFormat(String type) {
        EditText etValue = null;
        for (Binding b : bindings) {
            if ("value".equals(b.field.key)) {
                etValue = b.errorTarget;
                break;
            }
        }
        if (etValue == null) return;

        if (activeValueWatcher != null) {
            etValue.removeTextChangedListener(activeValueWatcher);
        }

        if ("percent".equals(type)) {
            setupPercentInput(etValue);
        } else {
            setupMoneyInput(etValue);
        }
    }

    private void setupPercentInput(EditText et) {
        String initial = et.getText().toString().replaceAll("[^0-9]", "");
        et.setText(formatPercentInput(initial));

        activeValueWatcher = new android.text.TextWatcher() {
            private String current = "";
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                et.setError(null);
                if (!s.toString().equals(current)) {
                    et.removeTextChangedListener(this);
                    String clean = s.toString().replaceAll("[^0-9]", "");
                    if (!clean.isEmpty()) {
                        long val = Long.parseLong(clean);
                        if (val > 100) clean = "100";
                    }
                    String formatted = formatPercentInput(clean);
                    current = formatted;
                    et.setText(formatted);
                    int pos = formatted.length();
                    if (formatted.endsWith(" %")) pos -= 2;
                    et.setSelection(Math.max(0, pos));
                    et.addTextChangedListener(this);
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        };
        et.addTextChangedListener(activeValueWatcher);
    }

    private String formatPercentInput(String clean) {
        return clean.isEmpty() ? "" : clean + " %";
    }

    private void setupMoneyInput(EditText et) {
        // Initial format if has value
        String initial = et.getText().toString().replaceAll("[^0-9]", "");
        if (!initial.isEmpty()) {
            et.setText(formatMoneyInput(initial));
        }

        activeValueWatcher = new android.text.TextWatcher() {
            private String current = "";
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                et.setError(null); // Clear error on change
                if (!s.toString().equals(current)) {
                    et.removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll("[^0-9]", "");
                    if (cleanString.length() > 12) cleanString = cleanString.substring(0, 12); // Limit to 999 billions

                    String formatted = formatMoneyInput(cleanString);

                    current = formatted;
                    et.setText(formatted);
                    
                    // Dat con tro truoc ky tu ' ₫'
                    int pos = formatted.length();
                    if (formatted.endsWith(" ₫")) pos -= 2;
                    et.setSelection(Math.max(0, pos));

                    et.addTextChangedListener(this);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        };
        et.addTextChangedListener(activeValueWatcher);
    }

    private String formatMoneyInput(String cleanString) {
        if (cleanString.isEmpty()) return "";
        try {
            long parsed = Long.parseLong(cleanString);
            java.text.DecimalFormat df = (java.text.DecimalFormat) java.text.NumberFormat.getInstance(new Locale("vi", "VN"));
            return df.format(parsed) + " ₫";
        } catch (Exception e) {
            return cleanString;
        }
    }

    private void addDate(LinearLayout parent, FormField f) {
        EditText et = new EditText(this);
        et.setBackgroundResource(R.drawable.bg_input);
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        et.setTextColor(ContextCompat.getColor(this, f.enabled ? R.color.tc_text_primary : R.color.tc_text_secondary));
        et.setTextSize(15f);
        et.setText(f.value);
        et.setHint(R.string.hint_pick_date);
        et.setFocusable(false);
        et.setClickable(f.enabled);
        et.setEnabled(f.enabled);
        et.setInputType(InputType.TYPE_NULL);
        et.setLayoutParams(fieldParams());
        et.setOnClickListener(v -> {
            et.setError(null);
            pickDate(et);
        });
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
        new DatePickerDialog(this, (view, year, month, day) -> {
            target.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day));
            validateDateTimeRealtime();
        },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void addSpinner(LinearLayout parent, FormField f) {
        Spinner sp = new Spinner(this);
        sp.setBackgroundResource(R.drawable.bg_spinner);
        sp.setPadding(dp(10), dp(6), dp(10), dp(6));
        sp.setEnabled(f.enabled);
        
        // Su dung ArrayList de co the thay doi phan tu "custom"
        List<String> options = new ArrayList<>(java.util.Arrays.asList(f.options));
        List<String> values = new ArrayList<>(java.util.Arrays.asList(f.optionValues));
        
        ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, options);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);

        int sel = -1;
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equals(f.value)) {
                sel = i;
                break;
            }
        }
        
        // Neu gia tri hien tai khong co trong list mac dinh (gia tri tu nhap truoc do)
        if (sel == -1 && !f.value.isEmpty()) {
            // Thay the cho item "custom" neu co, hoac add vao
            int customIdx = values.indexOf("custom");
            if (customIdx != -1) {
                options.set(customIdx, f.value);
                values.set(customIdx, f.value);
                sel = customIdx;
            }
        }
        
        if (sel != -1) sp.setSelection(sel);
        sp.setLayoutParams(fieldParams());
        parent.addView(sp);

        if (f.enabled) {
            sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    String val = values.get(position);
                    if ("custom".equals(val)) {
                        promptCustomInput(sp, options, values, position);
                    }
                    
                    // Logic dac thu cho Voucher: Doi dinh dang o "Gia tri giam" khi doi "Loai giam"
                    if (module == AdminModule.VOUCHERS && "type".equals(f.key)) {
                        updateVoucherValueFormat(val);
                    }
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        bindings.add(new Binding(f, () -> {
            int pos = sp.getSelectedItemPosition();
            return pos >= 0 && pos < values.size() ? values.get(pos) : "";
        }, null));
    }

    private void promptCustomInput(Spinner sp, List<String> options, List<String> values, int position) {
        EditText input = new EditText(this);
        input.setHint("Nhập đối tượng áp dụng...");
        input.setSingleLine(true);
        
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        int p = dp(20);
        container.setPadding(p, p/2, p, 0);
        container.addView(input);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Tự nhập đối tượng")
                .setView(container)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (!text.isEmpty()) {
                        options.set(position, text);
                        values.set(position, text);
                        ((ArrayAdapter<?>)sp.getAdapter()).notifyDataSetChanged();
                    }
                })
                .setNegativeButton("Huỷ", (dialog, which) -> {
                    // Reset ve lua chon dau tien neu huy
                    sp.setSelection(0);
                })
                .setCancelable(false)
                .show();
    }

    private void addSwitch(LinearLayout parent, FormField f, boolean spaced) {
        SwitchCompat sw = new SwitchCompat(this);
        sw.setText(f.label);
        sw.setTextColor(ContextCompat.getColor(this, f.enabled ? R.color.tc_text_primary : R.color.tc_text_secondary));
        sw.setTextSize(15f);
        sw.setChecked("true".equals(f.value));
        sw.setEnabled(f.enabled);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(spaced ? 18 : 4);
        sw.setLayoutParams(lp);
        parent.addView(sw);
        bindings.add(new Binding(f, () -> sw.isChecked() ? "true" : "false", null));
    }

    private void addTime(LinearLayout parent, FormField f) {
        EditText et = new EditText(this);
        et.setBackgroundResource(R.drawable.bg_input);
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        et.setTextColor(ContextCompat.getColor(this, f.enabled ? R.color.tc_text_primary : R.color.tc_text_secondary));
        et.setTextSize(15f);
        et.setText(f.value);
        et.setHint(R.string.hint_pick_time);
        et.setFocusable(false);
        et.setClickable(f.enabled);
        et.setEnabled(f.enabled);
        et.setInputType(InputType.TYPE_NULL);
        et.setLayoutParams(fieldParams());
        et.setOnClickListener(v -> {
            et.setError(null);
            pickTime(et);
        });
        parent.addView(et);
        bindings.add(new Binding(f, () -> et.getText().toString().trim(), et));
    }

    private void pickTime(EditText target) {
        Calendar c = Calendar.getInstance();
        String cur = target.getText().toString().trim();
        if (cur.contains(":")) {
            try {
                String[] p = cur.split(":");
                c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(p[0]));
                c.set(Calendar.MINUTE, Integer.parseInt(p[1]));
            } catch (Exception ignored) {}
        }
        new android.app.TimePickerDialog(this, (view, hour, minute) -> {
            target.setText(String.format(Locale.US, "%02d:%02d", hour, minute));
            validateDateTimeRealtime();
        },
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
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

        // Logic check logic date range cho Voucher
        if (module == AdminModule.VOUCHERS) {
            String start = values.get("startDate");
            String expiry = values.get("expiry");
            String startTime = values.get("startTime");
            String endTime = values.get("endTime");

            if (start != null && !start.isEmpty() && expiry != null && !expiry.isEmpty()) {
                int dateComp = start.compareTo(expiry);
                if (dateComp > 0) {
                    setErrorOnField("expiry", R.string.error_date_range);
                    return;
                } else if (dateComp == 0) {
                    // Cung ngày: Bắt buộc phải có giờ để phân biệt, hoặc báo lỗi nếu trùng cả giờ
                    if (startTime != null && !startTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                        int timeComp = startTime.compareTo(endTime);
                        if (timeComp >= 0) {
                            // Nếu trùng hoàn toàn cả ngày và giờ
                            setErrorOnField("endTime", R.string.error_time_range);
                            return;
                        }
                    } else {
                        // Nếu cùng ngày mà không có giờ, hoặc chỉ có 1 trong 2 giờ -> Coi như trùng thời điểm
                        setErrorOnField("expiry", R.string.error_date_range);
                        return;
                    }
                }
            }
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

    private void validateDateTimeRealtime() {
        if (module != AdminModule.VOUCHERS) return;
        
        String start = null, expiry = null, startTime = null, endTime = null;
        Binding expiryBinding = null, endTimeBinding = null;

        for (Binding b : bindings) {
            String val = b.reader.read();
            if ("startDate".equals(b.field.key)) start = val;
            else if ("expiry".equals(b.field.key)) { expiry = val; expiryBinding = b; }
            else if ("startTime".equals(b.field.key)) startTime = val;
            else if ("endTime".equals(b.field.key)) { endTime = val; endTimeBinding = b; }
        }

        if (start != null && !start.isEmpty() && expiry != null && !expiry.isEmpty()) {
            int dateComp = start.compareTo(expiry);
            if (dateComp > 0) {
                if (expiryBinding != null && expiryBinding.errorTarget != null) {
                    expiryBinding.errorTarget.setError(getString(R.string.error_date_range));
                }
            } else if (dateComp == 0) {
                if (startTime != null && !startTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
                    if (startTime.compareTo(endTime) >= 0) {
                        if (endTimeBinding != null && endTimeBinding.errorTarget != null) {
                            endTimeBinding.errorTarget.setError(getString(R.string.error_time_range));
                        }
                    } else {
                        if (endTimeBinding != null && endTimeBinding.errorTarget != null) endTimeBinding.errorTarget.setError(null);
                        if (expiryBinding != null && expiryBinding.errorTarget != null) expiryBinding.errorTarget.setError(null);
                    }
                }
            } else {
                if (expiryBinding != null && expiryBinding.errorTarget != null) expiryBinding.errorTarget.setError(null);
                if (endTimeBinding != null && endTimeBinding.errorTarget != null) endTimeBinding.errorTarget.setError(null);
            }
        }
    }

    private void setErrorOnField(String key, int stringRes) {
        for (Binding b : bindings) {
            if (key.equals(b.field.key) && b.errorTarget != null) {
                b.errorTarget.setError(getString(stringRes));
                b.errorTarget.requestFocus();
                break;
            }
        }
        Toast.makeText(this, stringRes, Toast.LENGTH_LONG).show();
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
