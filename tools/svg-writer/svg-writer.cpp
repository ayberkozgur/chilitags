/*******************************************************************************
 *   Copyright 2013-2014 EPFL                                                   *
 *   Copyright 2013-2014 Quentin Bonnard                                        *
 *                                                                              *
 *   This file is part of chilitags.                                            *
 *                                                                              *
 *   Chilitags is free software: you can redistribute it and/or modify          *
 *   it under the terms of the Lesser GNU General Public License as             *
 *   published by the Free Software Foundation, either version 3 of the         *
 *   License, or (at your option) any later version.                            *
 *                                                                              *
 *   Chilitags is distributed in the hope that it will be useful,               *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 *   GNU Lesser General Public License for more details.                        *
 *                                                                              *
 *   You should have received a copy of the GNU Lesser General Public License   *
 *   along with Chilitags.  If not, see <http://www.gnu.org/licenses/>.         *
 *******************************************************************************/

#include <cstdlib>
#include <string>
#include <chilitags.hpp>
#include "../src/Codec.hpp"
#include <iostream>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>

int main(int argc, char **argv)
{
	chilitags::Codec codec;
	unsigned char tag[6*6];
	char newcmd[512];

	//Landmarks
	/*
	const char* cmd = "sed -i 's/id=\\\"cell_%d_%d_%d\\\" style=\\\"fill:#000000;fill-opacity:1;stroke:none\\\"/id=\\\"cell_%d_%d_%d\\\" style=\\\"fill:#000000;fill-opacity:%d;stroke:none\\\"/g' landmark%d.svg";
	int tagno = 0;
	for(int landmark=1;landmark<=4;landmark++){
		sprintf(newcmd,"cp 4tag_template.svg landmark%d.svg",landmark);
		system(newcmd);
		for(int t=1;t<=4;t++){
			if(tagno == 14)
				codec.getTagEncodedId(16,tag);
			else
				codec.getTagEncodedId(tagno,tag);
			for(int y=0;y<6;y++)
				for(int x=0;x<6;x++){
					sprintf(newcmd,cmd,t,y+1,x+1,t,y+1,x+1,1-(int)tag[y*6+x],landmark);
					system(newcmd);
					std::cout << newcmd << std::endl;
				}
			tagno++;
		}
	}
	tagno = 20;
	for(int landmark=5;landmark<=30;landmark++){
		sprintf(newcmd,"cp 4tag_template.svg landmark%d.svg",landmark);
		system(newcmd);
		for(int t=1;t<=4;t++){
			codec.getTagEncodedId(tagno,tag);
			for(int y=0;y<6;y++)
				for(int x=0;x<6;x++){
					sprintf(newcmd,cmd,t,y+1,x+1,t,y+1,x+1,1-(int)tag[y*6+x],landmark);
					system(newcmd);
					std::cout << newcmd << std::endl;
				}
			tagno++;
		}
	}
	 */

	//Robots

	const char* cmd = "sed -i 's/id=\\\"cell_%d_%d_%d\\\" style=\\\"fill:#000000;fill-opacity:1;stroke:none\\\"/id=\\\"cell_%d_%d_%d\\\" style=\\\"fill:#000000;fill-opacity:%d;stroke:none\\\"/g' robot%d.svg";
	int tagno = 512;
	for(int robot=1;robot<=6;robot++){
		sprintf(newcmd,"cp 8tag_template.svg robot%d.svg",robot);
		system(newcmd);
		for(int t=1;t<=8;t++){
			codec.getTagEncodedId(tagno,tag);
			for(int y=0;y<6;y++)
				for(int x=0;x<6;x++){
					sprintf(newcmd,cmd,t,y+1,x+1,t,y+1,x+1,1-(int)tag[y*6+x],robot);
					system(newcmd);
					std::cout << newcmd << std::endl;
				}
			tagno++;
		}
	}

	return 0;

}
