package org.mmu.myfirstandroidapp;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.wolfram.alpha.WAEngine;
import com.wolfram.alpha.WAException;
import com.wolfram.alpha.WAPlainText;
import com.wolfram.alpha.WAPod;
import com.wolfram.alpha.WAQuery;
import com.wolfram.alpha.WAQueryResult;
import com.wolfram.alpha.WASubpod;
import com.wolfram.alpha.visitor.Visitable;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity
{
    
    //region 'Типы'
    
    @SuppressLint("StaticFieldLeak")
    private class WebDataDownloadTask extends AsyncTask<String, Void, Void>
    {
        private final WAEngine waEngine;
        private Map.Entry<Exception, String> error;
        private static final String UNKNOWN_WEB_ERROR_MES = "Ошибка загрузки данных по сети:";
        private static final String WOLFRAM_ALFA_ERROR_MES = "Ошибка движка WolframAlfa";
        
        @SuppressWarnings("deprecation")
        public WebDataDownloadTask(WAEngine engine)
        {
            waEngine = engine;
        }
        
        @SuppressWarnings("deprecation")
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            _cardList.clear();
            cardsAdapter.notifyDataSetChanged();
            progBar.setVisibility(View.VISIBLE);
            Log.d(LOG_TAG, "Начало загрузки веб-ресурса...");
        }
        
        @SuppressWarnings("deprecation")
        @Override
        protected Void doInBackground(String... request)
        {
            try
            {
                WAQuery query = waEngine.createQuery(request[0]);
                WAQueryResult res = waEngine.performQuery(query);
                if (res.isError())
                {
                    String err = res.getErrorMessage();
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
                for (WAPod pod : res.getPods())
                {
                    if (isCancelled())
                    {
                        return null;
                    }
                    if (pod.isError())
                    {
                        continue;
                    }
                    for (WASubpod subPod : pod.getSubpods())
                    {
                        for (Visitable element : subPod.getContents())
                        {
                            if (element instanceof WAPlainText)
                            {
                                sb.append(((WAPlainText) element).getText());
                            }
                        }
                    }
                    Map<String, String> m = new HashMap<>(2);
                    m.put(ADAPTER_TITLE, pod.getTitle());
                    m.put(ADAPTER_CONTENT, sb.toString());
                    _cardList.add(m);
                }
            }
            catch (Exception ex)        //приходится делать так, т.к. может быть SAXParseException
            {
                String mes = Objects.requireNonNull(ex.getMessage());
                error = new AbstractMap.SimpleEntry<>(ex, mes);
                Log.e(LOG_TAG, mes.isEmpty() ? (ex instanceof WAException ? WOLFRAM_ALFA_ERROR_MES :
                        UNKNOWN_WEB_ERROR_MES) : mes, ex);
            }
            return null;
        }
        
        @SuppressWarnings("deprecation")
        @Override
        protected void onPostExecute(Void unused)
        {
            super.onPostExecute(unused);
            if (error != null)
            {
                String mes = error.getValue();
                showSnackBar(error instanceof WAException ? WOLFRAM_ALFA_ERROR_MES : (mes.isEmpty() ?
                        UNKNOWN_WEB_ERROR_MES : mes));
                if (!mes.isEmpty())
                {
                    txtQuery.setError(mes);
                }
            }
            else
            {
                Log.d(LOG_TAG, "Загрузка Веб-ресурса завершена успешно");
                cardsAdapter.notifyDataSetChanged();
            }
            progBar.setVisibility(View.INVISIBLE);
        }
    }
    
    //endregion 'Типы'
    
    
    
    //region 'Поля и константы'
    
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    /**
     * APP NAME: NetologyQuery
     * <p>
     * USAGE TYPE: Personal/Non-commercial Only
     * </p>
     *
     * @apiNote The maximum numbers of Apps is 20 by default.
     * @implSpec An AppID must be supplied in all calls to the Wolfram|Alpha API. If you have
     * multiple applications using the Wolfram|Alpha API, each must have its own AppID.
     */
    private static final String WOLFRAM_API_KEY = "YOUR-KEY-HERE";
    public static final String ADAPTER_TITLE = "Title";
    public static final String ADAPTER_CONTENT = "Content";
    private static final List<Map<String, String>> _cardList = new ArrayList<>();
    
    private ActivityResultLauncher<Intent> voiceLauncher;
    private ProgressBar progBar;
    private WAEngine wolfEngine;
    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;
    /**
     * Обязательно пересоздавать задачу перед каждым вызовом!
     */
    private AsyncTask<String, Void, Void> downloadTask;
    private TextInputEditText txtQuery;
    private TextView txtGreeting;
    private View androidContentView;
    private SimpleAdapter cardsAdapter;
    
    //endregion 'Поля и константы'
    
    
    
    //region 'Обработчики'
    
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
        txtGreeting.setVisibility(View.GONE);
        this.initViews();
        wolfEngine = initWolfram();
        initTTS();
        voiceLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null)
                {
                    Log.e(LOG_TAG, "Обнаружена ошибка вызова голосовой активности");
                    return;
                }
                final var res = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
                txtQuery.setText(res);
                this.askWolframAsync(res);
            }
        );
        Log.w(LOG_TAG, "end of onCreate function");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_stop:
            {
                Log.d(LOG_TAG, String.format("Команда меню \"%s\"", item.getTitle()));
                if (isTtsReady)
                {
                    if (textToSpeech.stop() != TextToSpeech.SUCCESS)
                    {
                        Log.e(LOG_TAG,"Ошибка остановки фразы Текст-в-речь");
                        showSnackBar(getString(R.string.tts_stop_error));
                    }
                }
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
    
    private void onVoiceButtonClick(View view)
    {
        Log.d(LOG_TAG, "Нажата кнопка Запроса на голосовой ввод!");
        //progBar.setVisibility(progBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        _cardList.clear();
        cardsAdapter.notifyDataSetChanged();
        if (!isTtsReady)
        {
            initTTS();
        }
        // останавливаем TTS, чтобы он нас не перебивал вовремя работы с микрофоном
        if (isTtsReady)
        {
            textToSpeech.stop();
        }
        showVoiceInputDialog();
    }
    
    
    private boolean onEditorAction(TextView v, int actionId, KeyEvent event)
    {
        if (actionId == EditorInfo.IME_ACTION_DONE)
        {
            progBar.setVisibility(View.VISIBLE);    // подгон под тест 3
            _cardList.clear();
            cardsAdapter.notifyDataSetChanged();
            String text = Objects.requireNonNull(txtQuery.getText()).toString().replace("null", "");
            if (!text.isEmpty())
            {
                askWolframAsync(text);
            }
            return false;
        }
        return true;
    }
    
    /**
     * Обработчик нажатия на элемент списка результатов поиска (ответов wolfram)
     */
    final AdapterView.OnItemClickListener onListItemClicked_Handler = (parent, view, position, id) -> {
        if (!isTtsReady)
        {
            initTTS();
            showSnackBar(getString(R.string.tts_is_not_ready_error));
        }
        else
        {
            final var selectedItem = _cardList.get(position);
            final var title = selectedItem.getOrDefault(ADAPTER_TITLE, "N\\A");
            final var content = selectedItem.getOrDefault(ADAPTER_CONTENT, "");
            final var res = textToSpeech.speak(content, TextToSpeech.QUEUE_FLUSH, null, title);
            if (res == TextToSpeech.SUCCESS)
            {
                Log.d(LOG_TAG, String.format("Высказывание для заголовка '%s' добавлено в очередь TTS", title));
            }
            else
            {
                Log.e(LOG_TAG, String.format("Ошибка добавления в очередь TTS высказывания для заголовка '%s'", title));
                showSnackBar(getString(R.string.tts_add_queue_error));
            }
        }
    };
    
    //endregion 'Обработчики'
    
    
    
    //region 'Методы'
    
    /**
     * Метод инициализации движка WolframAlpha
     *
     * @return Готовый экземпляр движка запросов
     */
    private WAEngine initWolfram()
    {
        WAEngine result = new WAEngine();
        result.setAppID(WOLFRAM_API_KEY);
        result.addFormat("plaintext");
        return result;
    }
    
    /**
     * Метод инициализации движка распознавания речи
     *
     * @implSpec Для Samsung важно поменять в настройках предпочитаемый модуль с Samsung TTS на
     *      Google Speech Services, иначе настройки локали не применятся
     */
    private void initTTS()
    {
        textToSpeech = new TextToSpeech(this, (int status) -> {
            if (status == TextToSpeech.SUCCESS)
            {
                status = textToSpeech.setLanguage(Locale.US);
                if (status == TextToSpeech.LANG_MISSING_DATA || status == TextToSpeech.LANG_NOT_SUPPORTED)
                {
                    showTtsError(status);
                }
                else
                {
                    isTtsReady = true;
                    Log.d(LOG_TAG, "Интерфейс распознавания речи готов");
                }
            }
            else
            {
                showTtsError(status);
            }
        });
    }
    
    private void showTtsError(int status)
    {
        var mes = String.format(Locale.ROOT, "%s (Code: %d)", getString(R.string.tts_is_not_ready_error), status);
        Log.e(LOG_TAG, mes);
        showSnackBar(mes);
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
            new String[] { ADAPTER_TITLE, ADAPTER_CONTENT }, new int[] { R.id.card_title, R.id.card_content });
        lvCards.setAdapter(cardsAdapter);
        if (!_cardList.isEmpty())
        {
            txtGreeting.setVisibility(View.INVISIBLE);
        }
        final FloatingActionButton btVoiceInput = findViewById(R.id.voice_input_button);
        btVoiceInput.setOnClickListener(this::onVoiceButtonClick);
        txtQuery.setOnEditorActionListener(this::onEditorAction);
        lvCards.setOnItemClickListener(onListItemClicked_Handler);
    }
    
    /**
     * Метод отображения всплывающей подсказки
     */
    private void showSnackBar(String message)
    {
        Snackbar popup = Snackbar.make(this.androidContentView, message, Snackbar.LENGTH_INDEFINITE);
        txtQuery.setEnabled(false);
        // вариант для автотеста
//        popup.setAction(android.R.string.ok, view -> {
//            popup.dismiss();
//            //txtQuery.setEnabled(true);
//        });
        
        popup.setAction(R.string.repeat_button_caption, view -> {
            final String text  = Objects.toString(txtQuery.getText(), "");
            if (!text.isEmpty())
            {
                this.askWolframAsync(text);
            }
            popup.dismiss();
            txtQuery.setEnabled(true);
        });
        popup.show();
    }
    
    @SuppressWarnings("deprecation")
    private void askWolframAsync(String request)
    {
        if (request.isEmpty())
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
    
    /**
     * Метод вызова диалога преобразования Речи в текст
     */
    private void showVoiceInputDialog()
    {
        try
        {
            var voiceIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            voiceIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.request_hint));
            voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);
            this.voiceLauncher.launch(voiceIntent);
        }
        catch (RuntimeException ex)
        {
            final var mes = ex instanceof ActivityNotFoundException ?
                    getString(R.string.no_speech_to_text_service_error) :
                    getString(R.string.unexpected_speech_to_text_error) + ": " +
                            Objects.requireNonNullElse(ex.getMessage(), "");
            Log.e(LOG_TAG, mes);
            showSnackBar(mes);
        }
    }
    
    //endregion 'Методы'
    
}