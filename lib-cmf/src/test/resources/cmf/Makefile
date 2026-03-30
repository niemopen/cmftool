CT = /mnt/c/Windows/System32/cmd.exe /c c:\\work\\netbeans\\cmftool\\app-cmftool\\build\\install\\cmftool\\bin\\cmftool.bat

XSD_DIR := ../xsd6
XSD := $(wildcard $(XSD_DIR)/*.xsd)
CMF := $(patsubst $(XSD_DIR)/%.xsd,%.cmf,$(XSD))

.PHONY: all

all: $(CMF)

clean:
	rm -f $(CMF)

%.cmf: $(XSD_DIR)/%.xsd
	$(CT) x2m $< >/dev/null

