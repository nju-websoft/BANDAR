package dao;

import util.DBUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UriLabelIdDao {

	public static int getNodeNum(int datasetId) {
		DBUtil dbHelper = new DBUtil();
		String sql;
		sql= "select max(id) from uri_label_id where dataset_local_id=?";
		PreparedStatement psmt = null;
		try {
			psmt = dbHelper.conn.prepareStatement(sql);
			psmt.setInt(1, datasetId);
			ResultSet rs = psmt.executeQuery();
			if(rs.next())
				return rs.getInt(1);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				psmt.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return 0;
	}
}
