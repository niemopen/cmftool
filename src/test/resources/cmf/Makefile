CT = ../../../../build/install/cmftool/bin/cmftool
NIEM6 = \
  class.cmf\
  codeListBinding.cmf\
  component.cmf\
  dataProperty.cmf\
  datatypes.cmf\
  localTerm.cmf\
  namespace.cmf\
  objectProperty.cmf\
  propAssoc.cmf

all : $(NIEM5) $(NIEM6)

complexContent5.cmf: ../xsd5/complexContent.xsd
	$(CT) x2m $< > $@

$(NIEM6): %.cmf: ../xsd6/%.xsd
	$(CT) x2m $< > $@

clean :
	rm -f $(NIEM5) $(NIEM6)
