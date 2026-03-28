package com.example.currency_converter;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String INR = "INR";
    private static final String USD = "USD";
    private static final String JPY = "JPY";
    private static final String EUR = "EUR";

    private static final Map<String, Double> EXCHANGE_RATES = new HashMap<>();

    static {
        EXCHANGE_RATES.put(USD, 1.0);
        EXCHANGE_RATES.put(INR, 83.5);
        EXCHANGE_RATES.put(JPY, 149.5);
        EXCHANGE_RATES.put(EUR, 0.92);
    }

    private static final String[] CURRENCY_NAMES = {
            "Indian Rupee (INR)",
            "US Dollar (USD)",
            "Japanese Yen (JPY)",
            "Euro (EUR)"
    };

    private static final String[] CURRENCY_CODES = {INR, USD, JPY, EUR};

    private TextInputLayout amountInputLayout;
    private TextInputEditText amountInput;
    private TextInputLayout fromCurrencyLayout;
    private AutoCompleteTextView fromCurrencyDropdown;
    private TextInputLayout toCurrencyLayout;
    private AutoCompleteTextView toCurrencyDropdown;
    private TextView resultText;
    private TextView rateInfoText;
    private MaterialButton swapButton;
    private MaterialButton convertButton;

    private int fromIndex = 1;
    private int toIndex = 0;

    private final DecimalFormat resultFormatter = new DecimalFormat("#,##0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThemeHelper.applyTheme(this);

        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupToolbar();
        setupCurrencyDropdowns();
        setupListeners();
        updateExchangeRateInfo();
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        amountInputLayout = findViewById(R.id.amountInputLayout);
        amountInput = findViewById(R.id.amountInput);
        fromCurrencyLayout = findViewById(R.id.fromCurrencyLayout);
        fromCurrencyDropdown = findViewById(R.id.fromCurrencyDropdown);
        toCurrencyLayout = findViewById(R.id.toCurrencyLayout);
        toCurrencyDropdown = findViewById(R.id.toCurrencyDropdown);
        resultText = findViewById(R.id.resultText);
        rateInfoText = findViewById(R.id.rateInfoText);
        swapButton = findViewById(R.id.swapButton);
        convertButton = findViewById(R.id.convertButton);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                openSettings();
                return true;
            }
            return false;
        });
    }

    private void setupCurrencyDropdowns() {
        // Separate adapters for each dropdown to avoid shared state issues
        ArrayAdapter<String> fromAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                CURRENCY_NAMES
        );

        ArrayAdapter<String> toAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                CURRENCY_NAMES
        );

        fromCurrencyDropdown.setAdapter(fromAdapter);
        toCurrencyDropdown.setAdapter(toAdapter);

        // Ensure dropdown shows all items with proper width
        fromCurrencyDropdown.setDropDownWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        toCurrencyDropdown.setDropDownWidth(ViewGroup.LayoutParams.MATCH_PARENT);

        // Set initial values after layout is complete
        fromCurrencyDropdown.post(() -> {
            fromCurrencyDropdown.setText(CURRENCY_NAMES[fromIndex], false);
        });
        toCurrencyDropdown.post(() -> {
            toCurrencyDropdown.setText(CURRENCY_NAMES[toIndex], false);
        });
    }

    private void setupListeners() {
        // Add click listeners to ensure dropdown opens reliably
        fromCurrencyDropdown.setOnClickListener(v -> fromCurrencyDropdown.showDropDown());
        toCurrencyDropdown.setOnClickListener(v -> toCurrencyDropdown.showDropDown());

        fromCurrencyDropdown.setOnItemClickListener((parent, view, position, id) -> {
            fromIndex = position;
            updateExchangeRateInfo();
            performConversion();
        });

        toCurrencyDropdown.setOnItemClickListener((parent, view, position, id) -> {
            toIndex = position;
            updateExchangeRateInfo();
            performConversion();
        });

        swapButton.setOnClickListener(v -> swapCurrencies());

        convertButton.setOnClickListener(v -> performConversion());

        amountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performConversion();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void swapCurrencies() {
        int temp = fromIndex;
        fromIndex = toIndex;
        toIndex = temp;

        fromCurrencyDropdown.setText(CURRENCY_NAMES[fromIndex], false);
        toCurrencyDropdown.setText(CURRENCY_NAMES[toIndex], false);

        updateExchangeRateInfo();
        performConversion();
    }

    private void performConversion() {
        String amountStr = amountInput.getText() != null ? amountInput.getText().toString() : "";

        if (amountStr.isEmpty()) {
            resultText.setText("0.00");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            String fromCode = CURRENCY_CODES[fromIndex];
            String toCode = CURRENCY_CODES[toIndex];

            double result = convertCurrency(amount, fromCode, toCode);
            resultText.setText(resultFormatter.format(result));

        } catch (NumberFormatException e) {
            amountInputLayout.setError(getString(R.string.error_invalid_amount));
        }
    }

    private double convertCurrency(double amount, String fromCode, String toCode) {
        if (fromCode.equals(toCode)) {
            return amount;
        }

        double amountInUsd = amount / EXCHANGE_RATES.get(fromCode);
        return amountInUsd * EXCHANGE_RATES.get(toCode);
    }

    private void updateExchangeRateInfo() {
        String fromCode = CURRENCY_CODES[fromIndex];
        String toCode = CURRENCY_CODES[toIndex];

        double rate = convertCurrency(1.0, fromCode, toCode);
        String rateText = String.format(Locale.getDefault(),
                "1 %s = %s %s",
                fromCode,
                resultFormatter.format(rate),
                toCode);

        rateInfoText.setText(rateText);
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            openSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}