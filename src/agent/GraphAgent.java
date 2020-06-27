package agent;

import java.util.Map;

public interface GraphAgent {
	/**@param id Node id
	 * @return A list of integer array of length 2. int[0] represent the neighbor's id. the int[1] represent the id of the edge which link the Node_id and it's neighbor.
	 */
	public Map<Integer, Double> getNeighborInfo(Integer id);
}
