package org.squirrelsql.session.parser.kernel.completions;

public interface ErrorListener
{
	void errorDetected(String message, int line, int column);
}
