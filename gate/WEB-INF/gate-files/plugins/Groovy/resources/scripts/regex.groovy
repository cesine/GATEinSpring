// regex matching PR
// demonstrates use of the scriptParams map passed into the groovy script
//
// you need to set the scriptParams parameter of the PR to have:
// (1) a key called "regex" with a value of the java regex you want matching
// (2) a key called "type" with a value of the name of the annotation you want
//     created for each match

m = content =~ scriptParams.regex
while(m.find())
  outputAS.add(m.start(),m.end(),scriptParams.type,Factory.newFeatureMap())
