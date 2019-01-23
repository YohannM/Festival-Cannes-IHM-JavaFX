package vue;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;


public class Modifier extends AbstractAction
{
	Code fen;

	public Modifier(String name, Code fen)
	{
		super(name);
		this.fen = fen;
	}
	
	public void actionPerformed(ActionEvent arg0) 
	{
	
	}
}
