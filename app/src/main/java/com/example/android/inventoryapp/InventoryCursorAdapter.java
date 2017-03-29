package com.example.android.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.inventoryapp.InventoryContract.InventoryEntry;

import static android.R.attr.id;
import static java.security.AccessController.getContext;

/**
 * Created by paulstyslinger on 3/27/17.
 */

public class InventoryCursorAdapter extends CursorAdapter {


    //Constructor class
    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView priceTextView = (TextView) view.findViewById(R.id.price);
        TextView quantTextView = (TextView) view.findViewById(R.id.quantity);

        int idColumnIndex = cursor.getColumnIndex((InventoryEntry._ID));
        int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_NAME);
        int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_PRICE);
        int quantColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_QUANT);

        String itemName = cursor.getString(nameColumnIndex);
        String itemPrice = cursor.getString(priceColumnIndex);
        String itemQuant = cursor.getString(quantColumnIndex);

        // get necessary info to send to the Sold button for each item
        final long currentItemId = cursor.getLong(idColumnIndex);
        final Context currentContext = context;
        final int currentQuant = cursor.getInt(quantColumnIndex);

        nameTextView.setText(itemName);
        priceTextView.setText(context.getString(R.string.dollar_sign, itemPrice));
        quantTextView.setText(context.getString(R.string.in_stock, itemQuant));

        Button sellOneItemButton = (Button) view.findViewById(R.id.mark_sold_button);
        sellOneItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateQuantity(currentItemId, currentContext, currentQuant);
            }
        });
    }

    public void updateQuantity(long id, Context context, int quantity) {
        if (quantity > 0) {
            quantity -= 1;
            ContentValues values = new ContentValues();
            values.put(InventoryEntry.COLUMN_ITEM_QUANT, quantity);
            Uri uri = Uri.withAppendedPath(InventoryEntry.CONTENT_URI, Long.toString(id));

            context.getContentResolver().update(uri, values, null, null);
        }

    }

}
