package com.openmarket.db.actions;

import java.util.Date;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Response;

import com.google.common.collect.ImmutableMap;
import com.openmarket.db.Tables.Login;
import com.openmarket.db.Tables.Seller;
import com.openmarket.db.Tables.User;
import com.openmarket.tools.DataSources;
import com.openmarket.tools.TableConstants;
import com.openmarket.tools.Tools;

public class Actions {

	static final Logger log = LoggerFactory.getLogger(Actions.class);

	public static class UserActions {

		public static User createUserSimple(String email) {
			User user = User.create("email", email);
			Tools.writeRQL(user.toInsert());
			user = User.findFirst("email = ?", email);

			return user;
		}
		public static String userLogin(String email, String password, Response res) {

			User user = User.findFirst("email = ?", email);

			if (user == null) {
				throw new NoSuchElementException("Incorrect email");
			}

			String encryptedPassword = user.getString("password_encrypted");

			Boolean correctPass = Tools.PASS_ENCRYPT.checkPassword(password, encryptedPassword);

			if (correctPass) {
				return WebActions.setCookiesForLogin(user, res);

			} else {
				throw new NoSuchElementException("Incorrect password");
			}


		}
		
		public static String userLogout(String auth) {

			
			Login login = getLoginFromAuth(auth);

			if (login == null) {
				throw new NoSuchElementException("User not logged in");
			}
			
			long now = new Date().getTime();
			
			// Set the expire time to right now
			String update = Tools.toUpdate("login", login.getId().toString(), 
					"expire_time", now);

			Tools.writeRQL(update);

			return "Logged out";
		}
		
		public static Login getLoginFromAuth(String auth) {
			
			long now = new Date().getTime();
			
			Login login = Login.findFirst("session_id = ? and expire_time > ?" , auth, now);
			
			
			return login;
			
			
		}
		
		public static User getUserFromAuth(String auth) {
			
			Login login = getLoginFromAuth(auth);
			
			User user = User.findById(login.getString("user_id"));
			
			return user;
			
		}

		public static String sendSignUpEmail(User user) {


			String email = user.getString("email");

			String subject = "OpenMarket Sign Up";

			String token = Tools.generateSecureRandom();

			// Store that token in the user row
			//			User userUpdate = user.set("email_code", token);



			String update = Tools.toUpdate("user", user.getId().toString(), "email_code", token);

			Tools.writeRQL(update);

			String url = DataSources.SET_PASSWORD_URL() + "?token=" + token;
			
			Map<String, Object> vars = ImmutableMap.<String, Object>builder()
					.put("url", url)
					.build();

			String html = Tools.parseMustache(vars, DataSources.SIGNUP_EMAIL_TEMPLATE);

			String message = Tools.sendEmail(email, subject, html);
			
			return message;

		}

		public static User signupUserWithToken(String token, String password) {

			log.info("token = " + token);
			User user = User.findFirst("email_code = ?", token);

			if (user != null) {
				// Encrypt the password
				String encryptedPass = Tools.PASS_ENCRYPT.encryptPassword(password);

				String update = Tools.toUpdate("user", user.getId().toString(), 
						"password_encrypted", encryptedPass,
						"authenticated", true,
						"email_code", null);

				Tools.writeRQL(update);

//				String message = "Password set";

				// Now log the user in
				return user;

			} else {
				throw new NoSuchElementException("Incorrect email code");
			}


		}

	}


	public static class SellerActions {
		
		public static Seller createSellerSimple(String userId) {
			
			Seller seller = Seller.create("user_id", userId);
			Tools.writeRQL(seller.toInsert());
			seller = Seller.findFirst("user_id = ?", userId);

			return seller;
		}
		
		public static Seller getSeller(String userId) {
			Seller seller = Seller.findFirst("user_id = ?", userId);
			return seller;
		}
	}





	public static class WebActions {

		public static String setCookiesForLogin(User user, Response res) {

			Integer expireSeconds = TableConstants.EXPIRE_SECONDS;

			long now = new Date().getTime();

			long expireTime = now + expireSeconds*1000;


			String authenticatedSessionId = Tools.generateSecureRandom();

			// Not sure if this is necessary yet
			Boolean secure = false;

			// Store the users user in the DB, give them a session id
			Login login = Login.create("user_id", user.getId(),
					"session_id", authenticatedSessionId,
					"time_", now,
					"expire_time", expireTime);

			Tools.writeRQL(login.toInsert());


			// Set some cookies for that users login
			res.cookie("authenticated_session_id", authenticatedSessionId, expireSeconds, secure);
			res.cookie("uid", user.getId().toString(), expireSeconds, secure);
			//			String json = Tools.GSON2.toJson(cache);
			//			System.out.println(json);


			return "Logged in";




		}

	}
}
