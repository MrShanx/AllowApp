package com.example.donbaws.myfirstapp;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {

    //Container getActivity() variable
    private Context context;

    //Scroll view for EditText user_log_text
    private ScrollView mScrollView;

    //Views
    private TextView amount_left_text;
    private EditText user_log_text;
    private Button res_button, inc_button, exp_button;

    //math variables
    private double total_amount = 0.0;
    private double temp_amount = 0.0;
    private String all_logs;

    //variables for shared preferences file (SharedPref obj, Editor obj)
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    //returns current activity for Context
    private Activity getActivity() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //assign context = this
        context = getActivity();

        //set title of app to AllowApp
        setTitle("AllowApp");

        //components from activity_main.xml
            inc_button = (Button) findViewById((R.id.income_button));
            exp_button = (Button) findViewById((R.id.expense_button));
            res_button = (Button) findViewById((R.id.reset_button));

            //assigns text views from xml to text view objects in java
            amount_left_text = (TextView) findViewById(R.id.allowance_amount);
            user_log_text = (EditText) findViewById(R.id.user_log_textArea);
            //sets user_log_text to scrollable
            mScrollView = (ScrollView) findViewById(R.id.scroller_id);

        //add button listener for income, expense, and reset buttons
        inc_button.setOnClickListener(new promptListener());
        exp_button.setOnClickListener(new promptListener());
        res_button.setOnClickListener(new resetListener());

        //creates shared preference file
        // for persisting data using shared preferences
        sharedPref = context.getSharedPreferences("MyFirstApp.Settings", Context.MODE_PRIVATE);

        //Reads from sharedPreferences file upon startup
        //updates views with data loaded from shared preferences file
        readDataAndUpdateViews();
    }

    //action listener for user input (activity_input.xml) page
    private class promptListener implements View.OnClickListener {
        @Override
        public void onClick(View v){
            //determine if income or expense transaction from the button clicked
            final String clicked = determineTransaction(v);

            //get activity_input.xml view
            LayoutInflater li = LayoutInflater.from(context);
            View activity_inputView = li.inflate(R.layout.activity_input, null);

            //2 class imports to choose from***
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

            //set activity_input.xml to alertdialog builder
            alertDialogBuilder.setView(activity_inputView);

            //assign EditText View in xml (activity_input.xml) to user_input object in java
            final EditText user_input = (EditText) activity_inputView.findViewById(R.id.amount_input);

                //Limits user input to 2 decimal points in EditText View of (activity_input.xml)
                user_input.setFilters(new InputFilter[] {new DecimalDigitsInputFilter(5,2)});

            //set dialog message
            alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton("ADD",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    //get user input from pop-up(activity_input.xml)
                                    try {
                                        String temp_string = String.valueOf(user_input.getText());
                                        //parseDouble causes errors if string is null
                                        if(temp_string.length() == 0) {
                                            temp_amount = 0;
                                        }
                                        else {
                                            temp_amount = Double.parseDouble(String.valueOf(temp_string));
                                        }
                                    }catch(NullPointerException e) {
                                        //do nothing
                                    }
                                        //process only if amount entered is > 0
                                        if(temp_amount > 0) {
                                            //Add Allowance button clicked
                                            if (clicked.equalsIgnoreCase("income")) {
                                                deposit(temp_amount);
                                            }

                                            //Add Expense button clicked
                                            else if (clicked.equalsIgnoreCase("expense")) {
                                                if(total_amount > 0 ) {
                                                    withdraw(temp_amount);
                                                }
                                                else {
                                                    noMoneyAlert();
                                                }
                                            }

                                            //converts double to string and limits decimal numbers to 2
                                            String total_amount_converted = String.format("%,.2f", total_amount);
                                            updateAmountView("$" + total_amount_converted);
                                            updateLogView(all_logs);

                                            //persist data (write to shared preferences file) in each transaction
                                            saveData(total_amount_converted, all_logs);
                                        }
                                }
                            })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

            //create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();

            //this allows automatic pop-up of on-screen keyboard upon input prompt.
            //without having to touch the input TextView
            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

            //show it
            alertDialog.show();
        }
    }

    //deposit allowance
    private void deposit(Double amt) {
        //add new deposit log
        if(all_logs == null) {
            all_logs = "+$" + amt + " Allowance added***";
        }
        else {
            String temp = "\n" + "+$" + amt + " Allowance added***";
            all_logs += temp;
        }
        //add new amount to total
        total_amount += amt;
    }

    //withdraw allowance
    private void withdraw(Double amt) {
        //add new withdraw log
        if(all_logs == null) {
            all_logs = "-$" + amt + " Expense subtracted***";
        }
        else {
            String temp = "\n" + "-$" + amt + " Expense subtracted***";
            all_logs +=  temp;
        }
        //add new amount to total
        total_amount -= amt;
    }

    //write values to shared pref
    private void saveData(String amt_to_save, String log_to_save) {
        editor = sharedPref.edit();
        //putString(key, value) pair
        editor.putString("total_amount_saved", amt_to_save);
        editor.putString("all_logs_saved", log_to_save);
        editor.apply();
    }

    //read values from shared pref and update the views
    //ONLY updates views if values are present
    private void readDataAndUpdateViews() {
        //take values stored in shared preferences file (deletes record of them)
        String total_amount_saved = sharedPref.getString("total_amount_saved", "missing_amount");
        String all_logs_saved = sharedPref.getString("all_logs_saved", "missing_logs");

        //update views with values
        if((!(total_amount_saved.equalsIgnoreCase("missing_amount"))) &&
                (!(all_logs_saved.equalsIgnoreCase("missing_logs")))) {
                    updateAmountView("$"+ total_amount_saved);
                    updateLogView(all_logs_saved);

                //adding retrieved values (from shared preferences) to current variables (for continuity)
                //causes errors for app if ("missing_amount" is tried to parse into double)
                total_amount += Double.parseDouble(total_amount_saved);
                all_logs = all_logs_saved;
        }
    }

    //delete contents of shared pref
    private void deleteData() {
        editor = sharedPref.edit();
        //remove(key)
        editor.remove("total_amount_saved");
        editor.remove("all_logs_saved");
        editor.apply();
    }

    //updates Allowance amount view
    private void updateAmountView(String amt) {
        amount_left_text.setText(amt);
    }

    //updates User logs view
    private void updateLogView(String log) {
        user_log_text.setText(log);
        //shifts focus to bottom of log per log update
        //mScrollView.scrollTo(0, user_log_text.getBottom());
        scrollToBottom();
    }

    //scrolls to buttom of user_log_text after each transaction
    private void scrollToBottom() {
        mScrollView.post(new Runnable() {
            public void run() {
                mScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    //resets values/variables
    private void resetValues() {
        total_amount = 0;
        temp_amount = 0;
        all_logs = null;
    }

    //determine button pressed by View v
    private String determineTransaction(View v) {
        String button_clicked;
        switch(v.getId()) {
            case R.id.income_button:
                button_clicked = "income";
                break;
            case R.id.expense_button:
                button_clicked = "expense";
                break;
            default:
                throw new RuntimeException("Unknown button pressed");
        }
        return button_clicked;
    }

    private void noMoneyAlert() {
        new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Invalid Transaction")
                .setMessage("Please add more money")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        inc_button.performClick();
                    }
                })
                .show();
    }

    //reset button listener
    private class resetListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            new AlertDialog.Builder(context)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Reset Activity")
                    .setMessage("Are you sure you want to reset ALL activity?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //remove text from textviews
                            updateAmountView("Add allowance");
                            updateLogView(null);

                            //reset values of log and amount
                            resetValues();

                            //remove values written in sharedpreferences
                            deleteData();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }

    //Decimal filter for input dialog (activity_input.xml), limits input to two decimals
    //copied @stackoverflow
    public class DecimalDigitsInputFilter implements InputFilter {

        Pattern mPattern;

        DecimalDigitsInputFilter(int digitsBeforeZero, int digitsAfterZero) {
            mPattern=Pattern.compile("[0-9]{0," + (digitsBeforeZero-1) + "}+" +
                    "((\\.[0-9]{0," + (digitsAfterZero-1) + "})?)||(\\.)?");
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

            Matcher matcher=mPattern.matcher(dest);
            if(!matcher.matches())
                return "";
            return null;
        }
    }

}
