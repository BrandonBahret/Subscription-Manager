package com.example.brandon.SubscriptionsManager;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

public class SubscriptionsFragment extends Fragment {
    private SubscriptionsDatabase entriesDB = null;

    private LinearLayout subscriptionsContainer;
    private Typeface fontAwesome;
    private View mainView;
    private Context context;

    ArrayList<BecomesEmptyListener> listeners = new ArrayList<BecomesEmptyListener> ();
    ArrayList<OnSubscriptionClickListener> subscriptionClickListeners = new ArrayList<OnSubscriptionClickListener> ();

    public void setOnBecomesEmptyListener (BecomesEmptyListener listener) {
        this.listeners.add(listener);
    }

    public interface BecomesEmptyListener {
        void onBecomesEmpty();
    }

    public void setOnSubscriptionClickListener (OnSubscriptionClickListener listener)
    {
        // Store the listener object
        this.subscriptionClickListeners.add(listener);
    }

    public interface OnSubscriptionClickListener
    {
        void onSubscriptionClick(Subscriptions subscription, int index);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.subscriptions_fragment, container, false);
        ScrollView scrollView = (ScrollView)mainView.findViewById(R.id.subscriptions);
        subscriptionsContainer = (LinearLayout)scrollView.findViewById(R.id.subscriptionsContainer);

        context = getContext();

        fontAwesome = Typeface.createFromAsset(getActivity().getAssets(), "fontawesome-webfont.ttf");

        entriesDB = new SubscriptionsDatabase(getActivity());
        entriesDB.setOnDataChanged(new SubscriptionsDatabase.DataChangeListener() {
            @Override
            public void onDataChanged() {
                updateSubscriptionsFragment();
            }
        });

        updateSubscriptionsFragment();

        return mainView;
    }

    public void updateSubscriptionsFragment() {
        if(isAdded()) {
            String newText = getResources().getString(R.string.monthly_payment) +
                    String.valueOf(entriesDB.getTotalPayment());

            TextView paymentTextView = (TextView) mainView.findViewById(R.id.paymentTextView);
            paymentTextView.setText(newText);

            View[] subscriptionViews = convertSubscriptionsToViews(entriesDB.getSubscriptions());
            fillSubscriptionsInActivity(subscriptionViews);
        }
    }

    public View[] convertSubscriptionsToViews(final Subscriptions[] subscriptions){
        View[] results = new View[subscriptions.length];

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(0, 0, 0, 45);

        for(int i=0; i < results.length; ++i){
            final View newView = subscriptions[i].getView(context, fontAwesome);

            newView.setLayoutParams(layoutParams);

            newView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    int index = subscriptionsContainer.indexOfChild(view);
                    new DeleteSubscriptionDialog(context, index).show();
                    Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(10);

                    return true;
                }
            });

            newView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int index = subscriptionsContainer.indexOfChild(view);
                    Subscriptions subscription = entriesDB.getSubscriptions()[index];

                    for(OnSubscriptionClickListener listener: subscriptionClickListeners){
                        listener.onSubscriptionClick(subscription, index);
                    }
                }
            });

            results[i] = newView;
        }

        return results;
    }

    public class DeleteSubscriptionDialog extends AlertDialog.Builder {
        Context context;
        int index;
        AlertDialog dialog;

        public DeleteSubscriptionDialog(Context context, int index){
            super(context);
            this.context = context;
            this.index = index;
        }

        @Override
        public AlertDialog show() {
            super.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(getResources().getColor(R.color.red));

            return dialog;
        }

        @Override
        public AlertDialog create() {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Subscription will be deleted")
                    .setTitle("Are you sure?")
                    .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            entriesDB.removeRow(index);
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    });

            dialog = builder.create();

            return dialog;
        }
    }

    public void fillSubscriptionsInActivity(View[] subscriptions) {
        subscriptionsContainer.removeAllViews();

        if(subscriptions.length != 0) {
            for (View subscription : subscriptions) {
                subscriptionsContainer.addView(subscription);//, subscription.getLayoutParams());
            }
        }

        else {
            for(BecomesEmptyListener listener: listeners) {
                listener.onBecomesEmpty();
            }
        }
    }
}
