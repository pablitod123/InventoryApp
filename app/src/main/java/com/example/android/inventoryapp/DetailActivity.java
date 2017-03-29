package com.example.android.inventoryapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.app.LoaderManager;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryapp.InventoryContract.InventoryEntry;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import static com.example.android.inventoryapp.R.id.choose_image;
import static com.example.android.inventoryapp.R.id.deleteButton;
import static com.example.android.inventoryapp.R.id.image;
import static com.example.android.inventoryapp.R.id.image_view;
import static com.example.android.inventoryapp.R.id.orderButton;
import static com.example.android.inventoryapp.R.id.price;
import static com.example.android.inventoryapp.R.id.quantity;
import static java.lang.Integer.parseInt;

/**
 * Created by paulstyslinger on 3/26/17.
 */

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_INVENTORY_LOADER = 0;

    private Uri mCurrentItemUri;

    /**
     * EditText field to enter the item's name
     */
    private EditText mNameEditText;

    /**
     * EditText field to enter the item's quantity
     */
    private EditText mQuantEditText;

    /**
     * EditText field to enter the item's price
     */
    private EditText mPriceEditText;

    /**
     * TextView field tracking an item's quantity
     */
    private TextView mQuantCounterText;

    /**
     * Counter of current quantity
     */
    private int mCurrentQuantity;

    private Uri mImageUri;

    private Bitmap mBitmap;

    private boolean isGalleryPicture = false;

    private ImageView mImageView;

    private String uriImageString;

    private static int LOAD_IMAGE_ID = 1;

    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.inventoryapp";

    //Other TextViews
    private TextView mQuantText;
    private TextView mQuantInsertText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_layout);

        Intent intent = getIntent();
        mCurrentItemUri = intent.getData();

        // Find all relevant views
        mNameEditText = (EditText) findViewById(R.id.edit_item_name);
        mQuantEditText = (EditText) findViewById(R.id.edit_item_quantity);
        mPriceEditText = (EditText) findViewById(R.id.edit_item_price);
        mQuantCounterText = (TextView) findViewById(R.id.quant_counter);

        // The other TextViews
        mQuantText = (TextView) findViewById(R.id.text_item_quant);
        mQuantInsertText = (TextView) findViewById(R.id.text_item_quant_insert);

        mImageView = (ImageView) findViewById(R.id.image_view);


        Button orderButton = (Button) findViewById(R.id.orderButton);
        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText name  = (EditText) findViewById(R.id.edit_item_name);
                TextView quantity = (TextView) findViewById(R.id.quant_counter);
                String nameString = name.getText().toString().trim();
                String quantString = quantity.getText().toString().trim();

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setData(Uri.parse("mailto:"));
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, "Order Request");
                intent.putExtra(Intent.EXTRA_TEXT, "I need to order more of the following item: " + nameString + "\n" +
                        "I currently have " + quantString + " and need more." );
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });

        //Set OnClickListener for the button that marks the sale of an item
        Button saleButton = (Button) findViewById(R.id.saleButton);
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decrement(v);
            }
        });

        //Set OnClickListener for the button that marks that new items have come in
        Button newShipmentButton = (Button) findViewById(R.id.new_shipment);
        newShipmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                increment(v);
            }
        });

        //set OnClickListener for insert Button
        Button insertButton = (Button) findViewById(R.id.insertButton);
        insertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveItem();

            }
        });

        //set OnClickListener for image button
        Button imageButton = (Button) findViewById(R.id.choose_image);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                  //      android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                //startActivityForResult(galleryIntent, LOAD_IMAGE_ID);
                getImageFromGallery();
            }
        });

        //set OnClickListener for deleteButton
        Button deleteButton = (Button) findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteDialog();
            }
        });

        //Check if it's new or already created item, and display the appropriate content
        if (mCurrentItemUri == null) {
            // It's a new item
            setTitle(getString(R.string.detail_activity_new_item));

            //hide the views that only pertain to editing an item
            deleteButton.setVisibility(View.GONE);
            saleButton.setVisibility(View.GONE);
            newShipmentButton.setVisibility(View.GONE);
            orderButton.setVisibility(View.GONE);
            mQuantCounterText.setVisibility(View.GONE);
            mQuantText.setVisibility(View.GONE);
            mImageView.setVisibility(View.GONE);

        } else {
            //It's an item that's been clicked
            setTitle(getString(R.string.detail_activity_edit_item));

            insertButton.setText(R.string.save_item);
            mQuantEditText.setVisibility(View.GONE);
            mQuantInsertText.setVisibility(View.GONE);
            imageButton.setVisibility(View.GONE);

            getLoaderManager().initLoader(EXISTING_INVENTORY_LOADER, null, this);
        }

    }

    private void saveItem() {

        String nameString = mNameEditText.getText().toString().trim();
        String quantInsertString = mQuantEditText.getText().toString().trim();
        String quantEditString = mQuantCounterText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();

        if (mCurrentItemUri == null &&
                TextUtils.isEmpty(nameString) || TextUtils.isEmpty(priceString)) {
            Toast.makeText(this, getString(R.string.need_info),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidPositiveInteger(quantInsertString) || !isValidPositiveInteger(priceString)) {
                Toast.makeText(this, getString(R.string.invalid_input),
                        Toast.LENGTH_SHORT).show();
            return;
        }

        if (uriImageString.isEmpty()) {
            Toast.makeText(this, getString(R.string.invalid_input),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_ITEM_NAME, nameString);
        values.put(InventoryEntry.COLUMN_ITEM_PRICE, priceString);
        values.put(InventoryEntry.COLUMN_ITEM_QUANT, quantInsertString);
        values.put(InventoryEntry.COLUMN_ITEM_PHOTO, uriImageString);

        if (mCurrentItemUri == null) {
            // New item

            Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);

            // Show a toast message
            if (newUri == null) {
                Toast.makeText(this, getString(R.string.insert_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.insert_item_success),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // item already exists

            values.put(InventoryEntry.COLUMN_ITEM_QUANT, quantEditString);

            int rowsAffected = getContentResolver().update(mCurrentItemUri, values, null, null);

            // Show a toast message
            if (rowsAffected == 0) {
                Toast.makeText(this, getString(R.string.update_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.update_item_success),
                        Toast.LENGTH_SHORT).show();
            }
        }

        //exit the DetailActivity
        finish();
    }

    private void getImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), LOAD_IMAGE_ID);
    }


    @Override
    public android.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_ITEM_NAME,
                InventoryEntry.COLUMN_ITEM_PRICE,
                InventoryEntry.COLUMN_ITEM_QUANT,
                InventoryEntry.COLUMN_ITEM_PHOTO
        };

        return new CursorLoader(this,   // Parent activity context
                mCurrentItemUri,   // Provider content URI to query
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(android.content.Loader<Cursor> loader, Cursor cursor) {
        // Stop if cursor has fewer than one rows
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        if (cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_NAME);
            int quantColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_QUANT);
            int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_PRICE);
            int imageColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_PHOTO);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            int quant = cursor.getInt(quantColumnIndex);
            int price = cursor.getInt(priceColumnIndex);

            String imageString = cursor.getString(imageColumnIndex);
            Uri imageUri = Uri.parse(imageString);
            Bitmap bitmap = getBitmapFromUri(imageUri);
            ImageView imageView = (ImageView) findViewById(R.id.image_view);
            imageView.setImageBitmap(bitmap);


            // Update the views
            mNameEditText.setText(name);
            mQuantEditText.setText(Integer.toString(quant));
            mPriceEditText.setText(Integer.toString(price));
            mQuantCounterText.setText(Integer.toString(quant));
            mCurrentQuantity = quant;
            cursor.close();
        }
    }

    @Override
    public void onLoaderReset(android.content.Loader<Cursor> loader) {
        // Clear out all the data from the input fields.
        mNameEditText.setText("");
        mQuantEditText.setText("");
        mPriceEditText.setText("");
        mQuantCounterText.setText("");
        mCurrentQuantity = 0;
    }

    private void deleteItem() {
        if (mCurrentItemUri != null) {

            int rowsDeleted = getContentResolver().delete(mCurrentItemUri, null, null);

            if (rowsDeleted == 0) {
                Toast.makeText(this, getString(R.string.delete_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.delete_item_success),
                        Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    //Method to test if a string can be converted into a valid positive integer
    private boolean isValidPositiveInteger(String input) {
        boolean isValid = true;
        try {
            int parsedInt = Integer.parseInt(input);

            if (parsedInt < 0) {
                isValid = false;
            }
        } catch (NumberFormatException e) {
            isValid = false;
        }

        return isValid;
    }

    public void increment(View view) {
        mCurrentQuantity += 1;
        mQuantCounterText.setText(Integer.toString(mCurrentQuantity));
    }

    public void decrement(View view) {
        if (mCurrentQuantity > 0) {
            mCurrentQuantity -= 1;
            mQuantCounterText.setText(Integer.toString(mCurrentQuantity));
        } else {
            mCurrentQuantity = 0;
        }

    }

    /**
     * Prompt the user to confirm that they want to delete this pet.
     */
    private void showDeleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_message);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //Dismiss
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        if (requestCode == LOAD_IMAGE_ID && resultCode == Activity.RESULT_OK) {

            if (resultData != null) {
                mImageUri = resultData.getData();
                Log.e("DetailsActivity", "LOOK: " + mImageUri.toString());
                mBitmap = getBitmapFromUri(mImageUri);
                mImageView.setImageBitmap(mBitmap);
                uriImageString = getShareableImageUri().toString();
                isGalleryPicture = true;
            }
        }
    }


    private Bitmap getBitmapFromUri(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = null;
            if (parcelFileDescriptor != null) {
                fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            }
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
            }
            return image;
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Uri getShareableImageUri() {
        Uri imageUri;

        if (isGalleryPicture) {
            String filename = getFilePath();
            saveBitmapToFile(getCacheDir(), filename, mBitmap, Bitmap.CompressFormat.JPEG, 100);
            File imageFile = new File(getCacheDir(), filename);

            imageUri = FileProvider.getUriForFile(
                    this, FILE_PROVIDER_AUTHORITY, imageFile);

        } else {
            imageUri = mImageUri;
        }

        return imageUri;
    }

    public String getFilePath() {
        Cursor returnCursor =
                getContentResolver().query(mImageUri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);

        if (returnCursor != null) {
            returnCursor.moveToFirst();
        }
        String fileName = null;
        if (returnCursor != null) {
            fileName = returnCursor.getString(returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
        }
        if (returnCursor != null) {
            returnCursor.close();
        }
        return fileName;
    }


    public boolean saveBitmapToFile(File dir, String fileName, Bitmap bm,
                                    Bitmap.CompressFormat format, int quality) {
        File imageFile = new File(dir, fileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imageFile);
            bm.compress(format, quality, fos);
            fos.close();

            return true;
        } catch (IOException e) {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        return false;

    }



}
