package org.mmu.myfirstandroidapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.wolfram.alpha.WAEngine;
import com.wolfram.alpha.WAException;
import com.wolfram.alpha.WAPlainText;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    
    //region 'Типы'
    
    private class WebDataDownloadTask extends AsyncTask<String, Void, Void>
    {
        private final WAEngine waEngine;
        private Map.Entry<Exception, String> error;
        private static final String UNKNOWN_WEB_ERROR_MES = "Ошибка загрузки данных по сети:";
        private static final String WOLFRAM_ALFA_ERROR_MES = "Ошибка движка WolframAlfa";
        
        public WebDataDownloadTask(WAEngine engine)
        {
            waEngine = engine;
        }
        
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            _cardList.clear();
            cardsAdapter.notifyDataSetChanged();
            progBar.setVisibility(View.VISIBLE);
            Log.d(LOG_TAG, "Начало загрузки веб-ресурса...");
        }
        
        @Override
        protected Void doInBackground(String... request)
        {
            try
            {
                var query = waEngine.createQuery(request[0]);
                var res = waEngine.performQuery(query);
                if (res.isError())
                {
                    var err = res.getErrorMessage();
                    Log.w(LOG_TAG, err);
                    error = new AbstractMap.SimpleEntry<>(null, err);
                    return null;
                }
                if (!res.isSuccess())
                {
                    error = new AbstractMap.SimpleEntry<>(null, getString(R.string.request_hint));
                    return null;
                }
                if (res.getNumPods() <= 0)
                {
                    return null;
                }
                StringBuilder sb = new StringBuilder();
                for (var pod : res.getPods())
                {
                    if (isCancelled())
                    {
                        return null;
                    }
                    if (pod.isError())
                    {
                        continue;
                    }
                    for (var subPod : pod.getSubpods())
                    {
                        for (var element : subPod.getContents())
                        {
                            if (element instanceof WAPlainText)
                            {
                                sb.append(((WAPlainText) element).getText());
                            }
                        }
                    }
                    _cardList.add(Map.of(ADAPTER_KEY_TITLE, pod.getTitle(), ADAPTER_CONTENT, sb.toString()));
                }
            }
            catch (RuntimeException | WAException ex)
            {
                var mes = Objects.requireNonNullElse(ex.getMessage(), "");
                error = new AbstractMap.SimpleEntry<>(ex, mes);
                Log.e(LOG_TAG, mes.isEmpty() ? (ex instanceof WAException ? WOLFRAM_ALFA_ERROR_MES :
                        UNKNOWN_WEB_ERROR_MES) : mes, ex);
            }
            return null;
        }
        
        @Override
        protected void onPostExecute(Void unused)
        {
            super.onPostExecute(unused);
            progBar.setVisibility(View.GONE);
            if (error != null)
            {
                var mes = error.getValue();
                showSnackBar(error instanceof WAException ? WOLFRAM_ALFA_ERROR_MES : (mes.isEmpty() ?
                        UNKNOWN_WEB_ERROR_MES : mes));
                if (!mes.isBlank())
                {
                    txtQuery.setError(mes);
                }
            }
            else
            {
                Log.d(LOG_TAG, "Загрузка Веб-ресурса завершена успешно");
                cardsAdapter.notifyDataSetChanged();
            }
        }
    }
    
    //endregion 'Типы'
    
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    /**
     * APP NAME: NetologyQuery
     * <p>
     *     USAGE TYPE: Personal/Non-commercial Only
     * </p>
     * @apiNote The maximum numbers of Apps is 20 by default.
     * @implSpec An AppID must be supplied in all calls to the Wolfram|Alpha API. If you have
     *      multiple applications using the Wolfram|Alpha API, each must have its own AppID.
     */
    private static final String WOLFRAM_API_KEY = "HJJ8TL-E7TKGK6XE8";
    public static final String ADAPTER_KEY_TITLE = "Title";
    public static final String ADAPTER_CONTENT = "Content";
    private static final List<Map<String, String>> _cardList = new ArrayList<>(List.of(
            Map.of(
                    ADAPTER_KEY_TITLE, "Title 1",         // данные для автотеста 2
                    ADAPTER_CONTENT, "Content 1"          // данные для автотеста 2
            ),
            Map.of(
                    ADAPTER_KEY_TITLE, "Заголовок 1",
                    ADAPTER_CONTENT, "Данные 1"
            ),
            Map.of(
                    ADAPTER_KEY_TITLE, "Заголовок 2",
                    ADAPTER_CONTENT, "Данные 2"
            ),
            Map.of(
                    ADAPTER_KEY_TITLE, "Заголовок 3",
                    ADAPTER_CONTENT, "Данные 3"
            )
    ));
    
    private ProgressBar progBar;
    private WAEngine wolfEngine;
    
    /**
     * Обязательно пересоздавать задачу перед каждым вызовом!
     */
    private AsyncTask<String, Void, Void> downloadTask;
    private TextInputEditText txtQuery;
    private TextView txtGreeting;
    private View androidContentView;
    private SimpleAdapter cardsAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.w(LOG_TAG, "start of onCreate function");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    
        androidContentView = findViewById(android.R.id.content);
        progBar = findViewById(R.id.progress_bar);
        progBar.setVisibility(View.INVISIBLE);
        txtQuery = findViewById(R.id.txt_input);
        txtGreeting = findViewById(R.id.output);
        txtGreeting.setText("Ну, привет.");
        Log.d(LOG_TAG, "Привет, привет! Коли не шутишь...");
        String name = "Ivan", surname = "Ivanov";
        int age = 37;
        float height = 172.2f;
        String outInfo = String.format(Locale.ROOT, "name: %s surname: %s age: %d height: %.1f",
                name, surname, age, height);
        // Задание 2:
        txtGreeting.setText(outInfo);
        Log.i(LOG_TAG, outInfo);
        //
        this.initViews();
        wolfEngine = initWolfram();
        //
        Log.w(LOG_TAG, "end of onCreate function");
    }
    
    /**
     * Метод инициализации движка WolframAlpha
     *
     * @return Готовый экземпляр движка запросов
     */
    private WAEngine initWolfram()
    {
        var result = new WAEngine();
        result.setAppID(WOLFRAM_API_KEY);
        result.addFormat("plaintext");
        return result;
    }
    
    /**
     * Метод настройки виджетов
     */
    private void initViews()
    {
        final MaterialToolbar customToolBar = findViewById(R.id.top_toolbar);
        this.setSupportActionBar(customToolBar);
        final ListView lvCards = findViewById(R.id.card_list);
        cardsAdapter = new SimpleAdapter(getApplicationContext(), _cardList, R.layout.list_item,
                new String[]{ADAPTER_KEY_TITLE, ADAPTER_CONTENT}, new int[]{R.id.card_title, R.id.card_content});
        lvCards.setAdapter(cardsAdapter);
        if (!_cardList.isEmpty())
        {
            txtGreeting.setVisibility(View.INVISIBLE);
        }
        final FloatingActionButton btVoiceInput = findViewById(R.id.voice_input_button);
        btVoiceInput.setOnClickListener(view -> {
            Log.d(LOG_TAG, "Нажата кнопка Запроса на голосовой ввод!");
            progBar.setVisibility(progBar.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
            this.showSnackBar("А вы метод поиска дописали?");
        });
        txtQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE)
            {
                _cardList.clear();
                cardsAdapter.notifyDataSetChanged();
                var text  = Objects.requireNonNull(txtQuery.getText()).toString().replace("null", "");
                if (!text.isBlank())
                {
                    askWolframAsync(text);
                }
                return false;
            }
            return true;
        });
    }
    
    /**
     * Метод отображения всплывющей подсказки
     */
    private void showSnackBar(String message)
    {
        var popup = Snackbar.make(this.androidContentView, message, Snackbar.LENGTH_INDEFINITE);
        txtQuery.setEnabled(false);
        // вариант для автотеста
        popup.setAction(android.R.string.ok, view -> {
            popup.dismiss();
            txtQuery.setEnabled(true);
        });
//        popup.setAction(R.string.repeat_button_caption, view -> {
//            var text  = Objects.requireNonNull(txtQuery.getText()).toString().replace("null", "");
//            if (!text.isBlank())
//            {
//                this.askWolframAsync(text);
//            }
//            popup.dismiss();
//            txtQuery.setEnabled(true);
//        });
        popup.show();
    }
    
    private void askWolframAsync(String request)
    {
        if (request.isBlank())
        {
            showSnackBar("Пустые запросы запрещены!");
            return;
        }
        if (downloadTask != null && !downloadTask.isCancelled() && (downloadTask.getStatus() == AsyncTask.Status.RUNNING))
        {
            downloadTask.cancel(true);
        }
        downloadTask = new WebDataDownloadTask(wolfEngine).execute(request);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_stop:
            {
                Log.d(LOG_TAG, String.format("Команда меню \"%s\"", item.getTitle()));
                break;
            }
            case R.id.action_clear:
            {
                Log.d(LOG_TAG, String.format("Команда меню \"%s\"", item.getTitle()));
                //TODO: теоретически здесь может быть эксепшен
                Objects.requireNonNull(txtQuery.getText()).clear();
                _cardList.clear();
                cardsAdapter.notifyDataSetChanged();
                break;
            }
            default:
            {
                Log.w(LOG_TAG, "Неизвестная команда меню!");
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}