// A simple script to demonstrate the GATE Groovy script PR
// Copies annotations from one set to another

inputAS.get("Test").each{
  outputAS.add(it)
}