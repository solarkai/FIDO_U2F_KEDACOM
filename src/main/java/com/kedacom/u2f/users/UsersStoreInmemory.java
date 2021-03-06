/**
* Filename : TestFile.java
* Author : zhangkai
* Creation time : 2018年9月14日下午1:06:06 
* Description :
*/
package com.kedacom.u2f.users;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yubico.u2f.data.DeviceRegistration;
import com.yubico.u2f.exceptions.U2fBadInputException;

import com.kedacom.u2f.common.UserListNode;
import com.kedacom.u2f.common.UserInfo;
import com.kedacom.u2f.consts.U2fConsts;

public class UsersStoreInmemory implements IUserStore {

	private static Logger logger = LoggerFactory.getLogger(UsersStoreInmemory.class);

	/* <username,password> ,keep thread safe */
	private ConcurrentHashMap<String, String> userpwdMap;

	/*
	 * <username,hashmap of register_info<keyhandle,regdata.tojson()>, keep
	 * thread safe
	 */
	private ConcurrentHashMap<String, HashMap<String, String>> userRegInfoMap;

	/**
	 * 
	 */
	public UsersStoreInmemory() {
		userpwdMap = new ConcurrentHashMap<String, String>();
		userRegInfoMap = new ConcurrentHashMap<String, HashMap<String, String>>();

	}

	/**
	 * 
	 * @param username
	 * @param pwd
	 * @return
	 */
	public boolean addUser(String username, String pwd) {
		if (null != userpwdMap) {
			userpwdMap.put(username, pwd);
			userRegInfoMap.put(username, new HashMap<String, String>());
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 
	 * @param username
	 * @return
	 */
	public boolean removeUser(String username) {
		if (null != userpwdMap) {
			userpwdMap.remove(username);
			userRegInfoMap.remove(username);
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 
	 */
	public boolean modifyPassword(String username, String pwd){
		if (null != userpwdMap) {
			if(null != userpwdMap.get(username)){
				userpwdMap.put(username, pwd);
				return true;
			}else{
				logger.error("modifyPassword:no such user "+username);
			}
		}
		return false;
	}

	/**
	 * 
	 */
	@Override
	public boolean ifUserExists(String username) {
		String password = userpwdMap.get(username);
		if (null != password) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param username
	 * @param pwd
	 * @return
	 */
	public boolean checkUser(String username, String pwd) {
		String password = userpwdMap.get(username);
		if ((null != password) && (password.equals(pwd))) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * return RegInfo from user
	 * 
	 * @param username
	 * @return
	 */
	public List<DeviceRegistration> getUserRegInfo(String username) {
		List<DeviceRegistration> registrations = new ArrayList<DeviceRegistration>();
		HashMap<String, String> regmap = userRegInfoMap.get(username);
		try {
			for (String serialized : regmap.values()) {
				registrations.add(DeviceRegistration.fromJson(serialized));
			}
		} catch (U2fBadInputException e) {
			logger.error("getUserRegInfo error", e);
			return null;
		}
		return registrations;
	}

	/**
	 * bind the user with register data
	 */
	public boolean addUserRegInfo(String username, String keyhandle, String reginfojson) {
		HashMap<String, String> regmap = userRegInfoMap.get(username);
		if (null == regmap) {
			return false;
		} else {
			regmap.put(keyhandle, reginfojson);
			return true;
		}
	}

	/**
	 * unbind the register data for the user
	 */
	public boolean delUserRegInfo(String username, String keyhandle) {
		HashMap<String, String> regmap = userRegInfoMap.get(username);
		if (null == regmap) {
			logger.error("delUserRegInfo:get regmap of " + username + " is null.");
			return false;
		} else {
			regmap.remove(keyhandle);
			return true;
		}
	}

	/**
	 * if username equals "admin",return all the users;else just return the user
	 * with the username.
	 */
	public List<UserListNode> getUserList(String username) {
		CopyOnWriteArrayList<UserListNode> userNodeList = null;
		if (U2fConsts.ADMINNAME.equals(username)) {
			userNodeList = new CopyOnWriteArrayList<UserListNode>();
			for (String key : userpwdMap.keySet()) {
				UserListNode node = new UserListNode();
				UserInfo ui = new UserInfo();
				ui.setUsername(key);
				ui.setPassword(userpwdMap.get(key));
				node.setUserinfo(ui); // add userinfo
				HashMap<String, String> reginfo = userRegInfoMap.get(key);
				node.setUserreginfo(reginfo);
				userNodeList.add(node);
			}
		} else {
			if ((null != username) && (!"".equals(username))) {
				userNodeList = new CopyOnWriteArrayList<UserListNode>();
				UserListNode node = new UserListNode();
				UserInfo ui = new UserInfo();
				ui.setUsername(username);
				ui.setPassword(userpwdMap.get(username));
				node.setUserinfo(ui); // add userinfo
				HashMap<String, String> reginfo = userRegInfoMap.get(username);
				node.setUserreginfo(reginfo);
				userNodeList.add(node);
			}
		}
		return userNodeList;
	}

}
