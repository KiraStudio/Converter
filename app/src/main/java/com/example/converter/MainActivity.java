package com.example.converter;

    import androidx.appcompat.app.AppCompatActivity;

    import android.content.Context;
    import android.graphics.Color;
    import android.os.Bundle;
    import android.os.AsyncTask;

    import org.jsoup.Connection;
    import org.jsoup.Jsoup;
    import org.jsoup.nodes.Document;
    import org.jsoup.nodes.Element;
    import org.jsoup.select.Elements;

    import java.io.IOException;
    import java.util.ArrayList;
    import java.util.Collection;
    import java.util.Iterator;
    import java.util.LinkedHashMap;
    import java.util.LinkedList;
    import java.util.List;
    import java.util.Locale;
    import java.util.Objects;
    import java.util.regex.Matcher;
    import java.util.regex.Pattern;

    import android.view.View;
    import android.widget.*;

    public class MainActivity extends AppCompatActivity {
        LinkedHashMap<String, Float> map = new LinkedHashMap<String, Float>();
        LinkedHashMap<String, Integer> nominal = new LinkedHashMap<String, Integer>();

        String[] spinnerCurrency = {"RUB", "USD", "EUR", "GBP", "UAH", "BGN", "BRL", "HUF", "KRW", "HKD", "DKK", "AUD", "AZN", "INR", "KZT", "CAD", "KGS", "CNY", "MDL", "TMT",
        "NOK", "PLN", "RON", "XDR", "SGD", "TJS", "TRY", "UZS", "BYN", "AMD", "CZK", "SEK", "CHF", "ZAR", "JPY"};
        LinkedHashMap<String, String> name = new LinkedHashMap<String, String>();
        LinkedList<String> abr = new LinkedList<String>();

        Button calculateButton;

        String currencyFrom;
        String currencyTo;

        EditText enterValue;
        EditText resultText;

        TextView currentCurrency;
        TextView currentCurs;

        Document doc;
        Elements tableWth;
        Elements tr;

        Spinner spCurrencyFrom;
        Spinner spCurrencyTo;

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        enterValue = findViewById(R.id.enterValue);
        resultText = findViewById(R.id.result);
        currentCurrency = findViewById(R.id.currentCurrency);
        currentCurs = findViewById(R.id.currentCurs);
        calculateButton = findViewById(R.id.calculateButton);

        new NewThread().execute();
        ArrayAdapter<String> spinnerCurrencyAdapter = new ArrayAdapter<String>(getBaseContext(),android.R.layout.simple_spinner_item, spinnerCurrency);

        spinnerCurrencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spCurrencyFrom = findViewById(R.id.spinnerCurrencyFrom);
        spCurrencyTo = findViewById(R.id.spinnerCurrencyTo);
        spCurrencyFrom.setAdapter(spinnerCurrencyAdapter);
        spCurrencyTo.setAdapter(spinnerCurrencyAdapter);

        AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(getBaseContext(), adapterView.getSelectedItem().toString(), Toast.LENGTH_SHORT).show();
                currencyFrom = spCurrencyFrom.getSelectedItem().toString();
                currencyTo = spCurrencyTo.getSelectedItem().toString();
                if (!currencyFrom.equals("RUB")) {
                    for(int j = 0; j < name.size(); j++) {
                        if (currencyFrom.equals(spinnerCurrency[j + 1])) {
                            String abbreviate = spinnerCurrency[j + 1];
                            String name1 = name.get(abbreviate);
                            float value = map.get(abbreviate);
                            int coefficient = nominal.get(abbreviate);

                            currentCurs.setText(String.format(Locale.getDefault(), "%s %s %.4f   —   (%d единиц)\n", abbreviate, name1, value, coefficient));
                        }
                    }
                } else if (currencyFrom.equals("RUB")) {
                    currentCurs.setText("Наш славянский рубль");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        };
        spCurrencyFrom.setOnItemSelectedListener(itemSelectedListener);
        spCurrencyTo.setOnItemSelectedListener(itemSelectedListener);
    }

        public void Calculate(View v) {
            new NewThread().execute();

            if(tableWth == null) {
                currentCurrency.setText("\n\nУбедитесь, что вы подключены к сети Интернет! Приложение не сможет работать без активного соединения :(\n\nАвтор — vk.com/ermakov.kirill");
                calculateButton.setText("Обновить");
                return;
            }

            calculateButton.setText("Рассчитать");

            try {

                if (Float.parseFloat(enterValue.getText().toString().replace(",", ".")) > 0) {

                    if (currencyFrom.equals(currencyTo)) {
                        resultText.setText(String.format(Locale.getDefault(), "%s = %s", currencyFrom, currencyTo));
                        calculateButton.setText("Рассчитайте заново");

                    } else if (currencyFrom.equals("RUB")) {
                        float temp = Float.parseFloat(enterValue.getText().toString().replace(",", "."));
                        float currency = map.get(currencyTo);
                        float coefficient = nominal.get(currencyTo);
                        float result = temp / (currency / coefficient);
                        resultText.setText(String.valueOf(result));
                    } else if (!currencyFrom.equals("RUB") && !currencyTo.equals("RUB")) {
                        float temp = Float.parseFloat(enterValue.getText().toString().replace(",", "."));
                        float currency = map.get(currencyFrom);
                        float coefficient = nominal.get(currencyFrom);
                        float resultRub = temp * (currency / coefficient);
                        float currencyNext = map.get(currencyTo);
                        float coefficientNext = nominal.get(currencyTo);
                        float result = resultRub / (currencyNext / coefficientNext);
                        resultText.setText(String.valueOf(result));
                    } else if (!currencyFrom.equals("RUB") && currencyTo.equals("RUB")) {
                        float temp = Float.parseFloat(enterValue.getText().toString().replace(",", "."));
                        float currency = map.get(currencyFrom);
                        float coefficient = nominal.get(currencyFrom);
                        float resultRub = temp * (currency / coefficient);
                        resultText.setText(String.valueOf(resultRub));
                    }
                } else {
                    resultText.setText("Числа > 0");
                }
            }
            catch(Exception ex) {
                enterValue.setHint("Число?");
            }
    }

    class NewThread extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            currentCurrency.setText("Идет загрузка курса валют с ЦБ.\n\nПожалуйста, подождите...");
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                doc = Jsoup.connect("https://www.cbr.ru/currency_base/daily/?date_req=today").get();

                tableWth = doc.select("table[class=data]");
                tr = tableWth.select("tr").next();
                for (Element td:
                        tr) {
                    String value = td.text().substring(4); //получаем таблицу значений
                    String coefficient = value.substring(3, 9); //единицы
                    String key = value.substring(0, 3); //название валюты
                    abr.add(key);

                    Pattern p = Pattern.compile("\\s"); //регулярное выражение для поиска текущего курса к рублю
                    String[] fields = p.split(value);

                    Pattern p1 = Pattern.compile("\\d+"); //регулярное выражение для поиска коэффициента
                    Matcher m = p1.matcher(coefficient);
                    while(m.find()) {
                        int temp1 = Integer.parseInt(m.group());
                        nominal.put(key, temp1);
                    }

                    Pattern p2 = Pattern.compile("\\s[А-Яа-я()\\s+]*\\s");
                    Matcher m2 = p2.matcher(value);
                    while(m2.find()) {
                        name.put(key, m2.group());
                    }

                    float temp = Float.parseFloat(fields[fields.length - 1].replace(",", ".")); //берем последнее значение (курс валюты) из того, что нашло регулярное выражение
                    map.put(key, temp);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void params) {
            super.onPostExecute(params);

            if(tableWth == null) {
                currentCurrency.setText("\n\nУбедитесь, что вы подключены к сети Интернет! Приложение не сможет работать без активного соединения :(\n\nАвтор — vk.com/ermakov.kirill");
                calculateButton.setText("Обновить");
                return;
            }

            calculateButton.setText("Рассчитать");

            currentCurrency.setText("");
            for(int i = 0; i < map.size(); i++) {
                String abbreviate = abr.get(i);
                String name1 = name.get(abbreviate);
                float value = map.get(abbreviate);
                int coefficient = nominal.get(abbreviate);

                currentCurrency.append(String.format(Locale.getDefault(), "%s %s %.4f   —   (%d единиц)\n", abbreviate, name1, value, coefficient));
            }
            currentCurrency.append("\nАвтор — vk.com/ermakov.kirill\n");
        }
    }
}