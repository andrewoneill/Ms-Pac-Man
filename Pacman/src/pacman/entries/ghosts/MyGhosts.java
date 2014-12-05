package pacman.entries.ghosts;

import java.io.File;
import java.util.EnumMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pacman.controllers.Controller;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getActions() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., game.entries.ghosts.mypackage).
 */
public class MyGhosts extends Controller<EnumMap<GHOST,MOVE>>
{
	public static final int CROWDED_DISTANCE = 40;
	public static final int PACMAN_DISTANCE = 20;
	public static final int PILL_PROXIMITY = 20;
	
	private EnumMap<GHOST, MOVE> myMoves=new EnumMap<GHOST, MOVE>(GHOST.class);
	
	private MOVE[] moves = MOVE.values();
	private String myState = "chase";
	private String myEvent= "edible";
	private String myAction = "chase";

	public void FSM() {
		try {
			File fXmlFile = new File(
					"XML's/fsm.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			(doc).getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("state");

			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				Element eElement = (Element) nNode;
				System.out.println();
				if (eElement.getElementsByTagName("currentstate").item(0).getTextContent().equals(myState)
						&& eElement.getElementsByTagName("event").item(0).getTextContent().equals(myEvent)) {
					myAction = eElement.getElementsByTagName("action").item(0).getTextContent();
					myState = eElement.getElementsByTagName("newstate").item(0).getTextContent();
					System.out.println(myState);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public EnumMap<GHOST, MOVE> getMove(Game game, long timeDue)
	{
		myMoves.clear();
		
		FSM();
		
		//Place your game logic here to play the game as the ghosts
		
		return myMoves;
	}
}