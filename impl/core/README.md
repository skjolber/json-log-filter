# core module
This module contains the custom JSON filteres. 

# Parser patterns
The JSON filters within this project uses simple loops and switches to process JSON content.

The filters assume the JSON documents have valid syntax.

## Requirements
We want to create filters which do the following:

 * remove certain values or subtrees
 * hide certain values or subtrees
 * limit document size
 * limit text value size (i.e. base64 values)
 * remove whitespace
 * generate metrics (what was actually done to the document)

For specifying which values to hide or remove, the filters supports paths, i.e. a chain of field names like `/myRoot/myUser/myPassword`. As there is no field names in arrays, the parsers ignores array constructs, i.e. the path for "abcdef" in

```
{
  "myRoot": {
    "myUser": {
      "myPassword": "abcdef"
    }
  }           
}
```

is the same as

```
{
  "myRoot": [
      {
        "myUser": {
          "myPassword": "abcdef"
        }
      }
    ]
  }           
}
```

While this might seem like a really rough approach, this is sufficient for most JSON documents. This is only an disadvantage if the same array contains multiple different objects.

## Basic loop
Most parsers keeps track of the JSON syntax, i.e. `{`, `} `, `[`, `]`, `,` and `:`, 

```
while(offset < limit) {
    switch(chars[offset]) {
        case '[' :
        case '{' :
            break;
        case ']' :
        case '}' :
            break;
        case ',' :
            // ..
            break;
        case '"' :
            // .. skip over string (mind the escaping of double quote); scalar value or field name
            break;
        default :
          // skip over scalar values: numbers, null, booleans
    }
    offset++;
}
```

### Reading strings
Since we do not need to unscape the strings, scanning for unquoted double quotes is sufficient, i.e. stop after `f` in

```
"abcdef"
```

and after `i` in

```
"abcdef\"ghi"
```

while also no falling in the corner case

```
"abcdef\\"
```

Since ending a field name or value with a slash is unusual, we can speed up the algorithm: Look for a double quote, then see whether it was in fact escaped or not.

```
while(true) {
	do {
		offset++;
	} while(chars[offset] != '"');

	if(chars[offset - 1] != '\\') {
		return offset + 1;
	}

	// is there an even number of quotes behind?
	int slashOffset = offset - 2;
	while(chars[slashOffset] == '\\') {
		slashOffset--;
	}
	if((offset - slashOffset) % 2 == 1) {
		return offset + 1;
	}
}	
```

## Full path matching
Parsing the above path `/myRoot/myUser/myPassword` into an array, keeping track of the level and identifying field name strings, we can match the path as

```
int level = 0;
String[] pathElements = new String[]{null, "myRoot", "myUser", "myPassword"};

while(offset < limit) {
    switch(chars[offset]) {
        case '[' :
        case '{' :
            level++;
        
            break;
        case ']' :
        case '}' :
            level--;
            
            break;
        case ',' :
            // ..
            break;
        case '"' :
            // if this is a field name, compare it to the relevant path element
            String fieldName = ..;
            if(fieldName.equals(pathElements[level]) {
                if(level + 1 == pathElements.length) {
                  // remove or hide field value
                  continue
                }
                // fall through here if a match
            } else {
              // skip field value
            }
        default :
          // skip over scalar values: numbers, null, booleans
    }
    offset++;
}
```

In order to identify whether we are dealing with a field name or string scalar value, we look for trailing colons after the string end quote, i.e.

```
"myNumber": 123
```

is not confused with

```
"myNumber", 123
```

If the string is a field name and does not match the desired path elements, we skip the entire scalar or structure value, i.e. for

```
{
  "myRoot": [
      {
        "notMyUser": {
          ..
        },
        "notMyValue": 12356
      }
    ]
  }           
}
```

both `notMyUser` and `notMyValue` values are hidden from the main loop. This means that `level` is never higher than the number of elements in the path.

## Any path matching
Where we looking a specific field name at any level. While it against is sufficient to identify field name strings, skipping unknown structures is no longer an option.

## Max path matches
The feature is intended for documents where it is known that the targeted paths will only hit a certain number of times within the same document. This lets the filter simplify or skip processing the remainder of the document.

## Skipping objects or arrays
Keeping track of the current level and exiting at zero is all it takes:

```
int level = 1;

while(true) {
	switch(chars[offset]) {
		case '[' : {
			level++;
			break;
		}
		case ']' : {
			level--;
			
			if(level == 0) {
				return offset + 1;
			}
			break;
		}
		case '"' :
			offset = .. // end quote + 1
			continue;
		default :
	}
	offset++;
}
```

where the initial offset is +1 past the start bracket, and returns +1 past the end bracket.

## Max String size
For JSON document embedding base64 type data, we want to limit the size of the text values, i.e. output

```
{
  "myRoot": 
    {
      "mySignedPayload": "WWFkYXlhZGF5ZGExMjMyMTNhZHZkczIzNDMyNDMyMjM0MzI0NDIzMmRm"
    }
  }           
}
```

as

```
{
  "myRoot": 
    {
      "mySignedPayload": "WWFkYXlhZG.. 30 chars removed"
    }
  }           
}
```

Then the above loop can be simplified to

 * get hold of field names or text scalar values
 * check if the current string must be limited
 * identify whether the current string is a field value or not. 
 * check whether the string is actually reduced when also counting the additional ".. 30 chars removed" message
 * construct the truncated string
 
So it is not really necessary to keep track of level.

```
while(offset < limit) {
    switch(chars[offset]) {
        case '"' :
            int nextOffset = offset;
            // avoid escaped double quotes
            // also avoid to count escaped double slash as an escape character
            do {
                if(chars[nextOffset] == '\\') {
                    nextOffset++;
                }
                nextOffset++;
            } while(chars[nextOffset] != '"');

           // is string length is below max string size?
           if(nextOffset - offset < maxStringLength) {
                offset = nextOffset + 1;
                
                continue;
            }
            
            // if this is a field name, continue
            // otherwise limit the output of the current text value

            
        default :
          // skip over scalar values: numbers, null, booleans
    }
    offset++;
}
```
 
## Max size
In order to limit document size, we must be able to stop parsing the document when the max size is reached, and still output a valid JSON documents. So for the document

```
{
  "myRoot": [
      {
        "me": 1,
        "you": 2
      }
    ]
  }           
}
```

might be limited by size to


```
{
  "myRoot": [
      {
        "me": 1
      }
    ]
  }           
}
```
 
 then we we want to parse

```
{
  "myRoot": [
      {
        "me": 1
```

and "artificially" append the remaining end brackets, i.e.

```
      }
    ]
  }           
}
```
 
so that we end up with a valid JSON document. In other words we must track

 * the level, and 
 * for each level, whether we have an array or an object
 * the current output size
 * the size of the next item

to simply the parser, we keep a `mark` where the last safe exit point is;

 * brackets + 1
 * comma
 * end quotes + 1 (when the string is a value)

and skip over field names and scalar values. 

```
int maxSizeLimit = offset + maxSize;

int bracketLevel = 0;

boolean[] squareBrackets = new boolean[32];

int mark = 0;

loop:
while(offset < maxSizeLimit) {
    switch(chars[offset]) {
        case '{' :
        case '[' :
            // check corner case
            maxSizeLimit--;
            if(offset >= maxSizeLimit) {
                break loop;
            }

            squareBrackets[bracketLevel] = chars[offset] == '[';
            
            bracketLevel++;
            if(bracketLevel >= squareBrackets.length) {
                boolean[] next = new boolean[squareBrackets.length + 32];
                System.arraycopy(squareBrackets, 0, next, 0, squareBrackets.length);
                squareBrackets = next;
            }
            
            offset++;
            mark = offset;

            continue;
        case '}' :
        case ']' :
            bracketLevel--;
            maxSizeLimit++;
            
            offset++;
            mark = offset;

            continue;
        case ',' :
            mark = offset;
            break;
        case '"' :
            
            // avoid escaped double quotes
            // also avoid to count escaped double slash an escape character
            do {
                if(chars[offset] == '\\') {
                    offset++;
                }
                offset++;
            } while(chars[offset] != '"');
            
        default : {
            // some kind of value
            // do nothing
        }
    }
    offset++;
}
```

then the output is the sum of 

 * copy offset -> mark range
 * brackets which close the structure (squareBrackets + level)

Scalar values not marked in the main loop, rather a small check is performed before finalizing the mark, to see whether the included range did end in a full scalar value.

# Whitespace removal
As only strings should contain whitespace

 * locate strings and skip them, otherwise
 * skip all whitespace characters
 
In order to avoid flushing every single character to the output, we keep track of a `flushOffset` variable which tracks progress in writing to the output. Writing to the output is necessary whenever encountering whitespace:

```
int flushOffset = offset;

while(offset < limit) {
	char c = chars[offset];
	if(c == '"') {
		do {
			if(chars[offset] == '\\') {
				offset++;
			}
			offset++;
		} while(chars[offset] != '"');
	} else if(c <= 0x20) {
		// skip this char and any other whitespace
		output.append(chars, flushOffset, offset - start);
		do {
			offset++;
		} while(chars[offset] <= 0x20);
		
		flushOffset = offset;
		
		continue;
	}
	offset++;
}
output.append(chars, flushOffset, offset - flushOffset);
```

The `0x20` test for whitespace is sufficient as space, tab and newline are all characters below or equal to `0x20` and at the same time the only legal whitespaces.

