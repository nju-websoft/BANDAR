package dao;

import util.DBUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TripleDao {

	public static int getPredicate(int datasetId, int subject, int object){
		DBUtil dbHelper = new DBUtil();
		String sql;
		//------------running example------------------
		sql = "select predicate from triple where dataset_local_id=? and `subject`=? and object=? limit 1";
		//------------running example------------------
		PreparedStatement psmt = null;
		try {
			psmt = dbHelper.conn.prepareStatement(sql);
			psmt.setInt(1, datasetId);
			psmt.setInt(2, subject);
			psmt.setInt(3, object);
			ResultSet rs = psmt.executeQuery();
			if(rs.next())
				return rs.getInt(1);
			psmt.setInt(2, object);
			psmt.setInt(3, subject);
			ResultSet rs2 = psmt.executeQuery();
			if(rs2.next())
				return -rs2.getInt(1);
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
