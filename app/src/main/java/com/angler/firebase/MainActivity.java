package com.angler.firebase;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import java.util.List;

public class MainActivity extends Activity {

   private static final String TAG = "GoogleActivity";
   private static final int RC_SIGN_IN = 9001;

   private GoogleSignInClient mGoogleSignInClient;
   private FirebaseAuth mAuth;

   SignInButton mSignInButton;
   LoginButton FBloginButton;
   private TwitterLoginButton TWLoginButton;

   private CallbackManager mCallbackManager;

   @Override
   protected void onCreate( Bundle savedInstanceState ) {
      super.onCreate( savedInstanceState );
      FacebookSdk.sdkInitialize( this );

      twitterInit();


      setContentView( R.layout.activity_main );

      mSignInButton = findViewById( R.id.sign_in_button );
      FBloginButton = findViewById( R.id.loginButton );
      TWLoginButton = findViewById( R.id.twitterloginButton );


//Get KeyHash for Facebook android
      /*try {
         PackageInfo info = getPackageManager().getPackageInfo(
                 "com.angler.firebase",
                 PackageManager.GET_SIGNATURES );
         for( Signature signature : info.signatures ) {
            MessageDigest md = MessageDigest.getInstance( "SHA" );
            md.update( signature.toByteArray() );
            Log.d( "KeyHash:", Base64.encodeToString( md.digest(), Base64.DEFAULT ) );
         }
      } catch( PackageManager.NameNotFoundException e ) {

      } catch( NoSuchAlgorithmException e ) {

      }*/


      mAuth = FirebaseAuth.getInstance();

      google();
      facebook();
      twitter();
   }

   private void twitterInit() {
      // Configure Twitter SDK
      TwitterAuthConfig authConfig = new TwitterAuthConfig(
              getString( R.string.twitter_consumer_key ),
              getString( R.string.twitter_secret_key ) );

      TwitterConfig twitterConfig = new TwitterConfig.Builder( this )
              .twitterAuthConfig( authConfig )
              .build();

      Twitter.initialize( twitterConfig );
   }

   private void twitter() {

      TWLoginButton.setCallback( new Callback<TwitterSession>() {
         @Override
         public void success( Result<TwitterSession> result ) {
            Log.d( TAG, "twitterLogin:success" + result );
            handleTwitterSession( result.data );
         }

         @Override
         public void failure( TwitterException exception ) {
            Log.w( TAG, "twitterLogin:failure", exception );
            //updateUI( null );
         }
      } );
   }

   private void facebook() {
      mCallbackManager = CallbackManager.Factory.create();

      FBloginButton.setReadPermissions( "email", "public_profile" );

      FBloginButton.registerCallback( mCallbackManager, new FacebookCallback<LoginResult>() {
         @Override
         public void onSuccess( LoginResult loginResult ) {
            Log.d( TAG, "facebook:onSuccess:" + loginResult );
            handleFacebookAccessToken( loginResult.getAccessToken() );
         }

         @Override
         public void onCancel() {
            Log.d( TAG, "facebook:onCancel" );
         }

         @Override
         public void onError( FacebookException error ) {
            Log.d( TAG, "facebook:onError", error );
         }
      } );

   }

   private void google() {
      GoogleSignInOptions gso = new GoogleSignInOptions.Builder( GoogleSignInOptions.DEFAULT_SIGN_IN )
              .requestIdToken( getString( R.string.default_web_client_id ) )
              .requestEmail()
              .build();

      mGoogleSignInClient = GoogleSignIn.getClient( this, gso );

      mSignInButton.setOnClickListener( new View.OnClickListener() {
         @Override
         public void onClick( View view ) {
            signIn();
         }
      } );
   }


   private void signIn() {
      Intent signInIntent = mGoogleSignInClient.getSignInIntent();
      startActivityForResult( signInIntent, RC_SIGN_IN );
   }


   @Override
   public void onActivityResult( int requestCode, int resultCode, Intent data ) {
      super.onActivityResult( requestCode, resultCode, data );

      // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
      if( requestCode == RC_SIGN_IN ) {
         Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent( data );
         try {
            // Google Sign In was successful, authenticate with Firebase
            GoogleSignInAccount account = task.getResult( ApiException.class );
            firebaseAuthWithGoogle( account );
         } catch( ApiException e ) {
            // Google Sign In failed, update UI appropriately
            Log.w( TAG, "Google sign in failed", e );
            // [START_EXCLUDE]
            //  updateUI(null);
            // [END_EXCLUDE]
         }
      } else if( requestCode == TwitterAuthConfig.DEFAULT_AUTH_REQUEST_CODE ) {
         TWLoginButton.onActivityResult( requestCode, resultCode, data );
      } else {

         mCallbackManager.onActivityResult( requestCode, resultCode, data );
      }
   }


   private void firebaseAuthWithGoogle( GoogleSignInAccount acct ) {
      Log.d( TAG, "firebaseAuthWithGoogle:" + acct.getId() );
      AuthCredential credential = GoogleAuthProvider.getCredential( acct.getIdToken(), null );
      mAuth.signInWithCredential( credential )
              .addOnCompleteListener( this, new OnCompleteListener<AuthResult>() {
                 @Override
                 public void onComplete( @NonNull Task<AuthResult> task ) {
                    if( task.isSuccessful() ) {
                       // Sign in success, update UI with the signed-in user's information
                       Log.d( TAG, "signInWithCredential:success" );
                       FirebaseUser user = mAuth.getCurrentUser();
                       Log.e( "Display Name", user.getDisplayName() );
                       Log.e( "Display Email", user.getEmail() );
                       Log.e( "Display phone_number", user.getPhotoUrl().toString() );
                       user.getProviderData();

                       List<UserInfo> aList = ( List<UserInfo> ) user.getProviderData();

                       for( UserInfo aUser : aList ) {
                          Log.e( "Display is verified", "" + aUser.isEmailVerified() );
                       }
                       //  user.
                       //  updateUI(user);
                    } else {
                       // If sign in fails, display a message to the user.
                       Log.w( TAG, "signInWithCredential:failure", task.getException() );
                       //  Snackbar.make(findViewById(R.id.main_layout), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                       //  updateUI(null);
                    }

                 }
              } );
   }


   private void handleFacebookAccessToken( AccessToken token ) {
      Log.d( TAG, "handleFacebookAccessToken:" + token );

      AuthCredential credential = FacebookAuthProvider.getCredential( token.getToken() );
      mAuth.signInWithCredential( credential )
              .addOnCompleteListener( this, new OnCompleteListener<AuthResult>() {
                 @Override
                 public void onComplete( @NonNull Task<AuthResult> task ) {
                    if( task.isSuccessful() ) {
                       // Sign in success, update UI with the signed-in user's information
                       Log.d( TAG, "signInWithCredential:success" );
                       FirebaseUser user = mAuth.getCurrentUser();

                       Log.e( "FB Login user", user.getDisplayName() );
                       Log.e( "FB Login Email", user.getEmail() );
                       Log.e( "FB Login photo", user.getPhotoUrl().toString() );
                       FirebaseUserMetadata amata = user.getMetadata();
                       //    Log.e( "FB Login phone", user.getPhoneNumber() );

                       List<UserInfo> aList = ( List<UserInfo> ) user.getProviderData();

                       for( UserInfo aUser : aList ) {
                          Log.e( "Display is verified", "" + aUser.isEmailVerified() );
                       }
                    } else {
                       // If sign in fails, display a message to the user.
                       Log.w( TAG, "signInWithCredential:failure", task.getException() );
                       Toast.makeText( MainActivity.this, "Authentication failed.",
                               Toast.LENGTH_SHORT ).show();

                    }

                 }
              } );
   }


   // [START auth_with_twitter]
   private void handleTwitterSession( TwitterSession session ) {
      Log.d( TAG, "handleTwitterSession:" + session );

      AuthCredential credential = TwitterAuthProvider.getCredential(
              session.getAuthToken().token,
              session.getAuthToken().secret );

      mAuth.signInWithCredential( credential )
              .addOnCompleteListener( this, new OnCompleteListener<AuthResult>() {
                 @Override
                 public void onComplete( @NonNull Task<AuthResult> task ) {
                    if( task.isSuccessful() ) {
                       // Sign in success, update UI with the signed-in user's information
                       Log.d( TAG, "signInWithCredential:success" );
                       FirebaseUser user = mAuth.getCurrentUser();

                       Log.e( "Display Name", user.getDisplayName() );
                       Log.e( "Display phone_number", user.getPhotoUrl().toString() );
                       //  Log.e( "Display phone_number", user.getPhoneNumber() );
                       user.getProviderData();

                       List<UserInfo> aList = ( List<UserInfo> ) user.getProviderData();

                       for( UserInfo aUser : aList ) {
                          Log.e( "Display is verified", "" + aUser.isEmailVerified() );
                       }
                       //   updateUI(user);
                    } else {
                       // If sign in fails, display a message to the user.
                       Log.w( TAG, "signInWithCredential:failure", task.getException() );
                       Toast.makeText( MainActivity.this, "Authentication failed.",
                               Toast.LENGTH_SHORT ).show();

                    }
                 }
              } );
   }
}
