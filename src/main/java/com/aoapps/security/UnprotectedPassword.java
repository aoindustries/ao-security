/*
 * ao-security - Best-practices security made usable.
 * Copyright (C) 2021  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-security.
 *
 * ao-security is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-security is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-security.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoapps.security;

import com.aoapps.lang.function.ConsumerE;
import com.aoapps.lang.function.FunctionE;
import com.aoapps.lang.function.PredicateE;
import com.aoapps.lang.function.SupplierE;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Unlike {@link Password}, which goes out of its way to protect the password, an unprotected password provides access
 * to the password value.  This is intended for when the password needs to be accessible to the application, such as
 * providing a generated password to the user.
 *
 * @author  AO Industries, Inc.
 */
public class UnprotectedPassword extends Password {

	private final static Logger logger = Logger.getLogger(UnprotectedPassword.class.getName());

	/**
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #UnprotectedPassword(java.util.function.Supplier)
	 */
	private static <Ex extends Throwable> char[] generatePassword(SupplierE<? extends char[],Ex> generator) throws Ex {
		// Discard any passwords that are generated as all-zero (in the small chance)
		final int TRIES = 100;
		for(int i = 0; i < TRIES; i++) {
			char[] password = generator.get();
			if(password == null) throw new IllegalArgumentException("Generator created null password");
			if(password.length == 0) throw new IllegalArgumentException("Generator created empty password");
			if(!SecurityUtil.slowAllZero(password)) {
				return password;
			}
			logger.warning("Generator created all-zero password, discarding and trying again");
		}
		// Generator is broken; don't loop forever
		throw new IllegalArgumentException("Generator is only creating all-zero passwords, tried " + TRIES + " times");
	}

	/**
	 * @param  password  Is zeroed before this method returns.  If the original password is
	 *                   needed, pass a copy to this method.
	 *
	 * @throws IllegalArgumentException when {@code password == null || password.length == 0} or when {@code password}
	 *                                  is already destroyed (contains all zeroes).
	 */
	public UnprotectedPassword(char[] password) throws IllegalArgumentException {
		super(password);
	}

	/**
	 * Generates a new password using the provided password generator.
	 * <p>
	 * The password will never be all-zeroes, since this would conflict with the representation
	 * of already destroyed.  In the unlikely event the generator creates an all-zero
	 * password, the password will be discarded and another will be generated.  We do recognize that
	 * disallowing certain values from the password space may provide an advantage to attackers
	 * (i.e. Enigma), losing the all-zero password is probably a good choice anyway.
	 * </p>
	 *
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 */
	public <Ex extends Throwable> UnprotectedPassword(SupplierE<? extends char[],Ex> generator) throws Ex {
		this(generatePassword(generator));
	}

	/**
	 * Generates a new password using the default password generator
	 * and the provided {@link Random} source.
	 */
	public UnprotectedPassword(Random random) {
		this(() -> new SmallIdentifier(random).toCharArray());
	}

	/**
	 * Generates a new password using the default password generator.
	 */
	public UnprotectedPassword() {
		this(Identifier.secureRandom);
	}

	/**
	 * Copy constructor.
	 *
	 * @see  #clone()
	 */
	private UnprotectedPassword(UnprotectedPassword other) {
		super(other);
	}

	@Override
	@SuppressWarnings({"CloneDeclaresCloneNotSupported", "CloneDoesntCallSuperClone"})
	public UnprotectedPassword clone() {
		return new UnprotectedPassword(this);
	}

	/**
	 * @return  The caller must zero this array once no longer needed.
	 *
	 * @throws IllegalStateException when {@link #isDestroyed()}
	 *
	 * @see  #invoke(com.aoapps.lang.function.FunctionE)
	 * @see  #accept(com.aoapps.lang.function.ConsumerE)
	 * @see  #test(com.aoapps.lang.function.PredicateE)
	 */
	char[] getPassword() throws IllegalStateException {
		synchronized(password) {
			if(isDestroyed()) throw new IllegalStateException("Password is already destroyed");
			return Arrays.copyOf(password, password.length);
		}
	}

	/**
	 * Calls a function, providing a copy of the password.
	 * The copy of the password is zeroed once the function returns.
	 *
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @throws IllegalStateException when {@link #isDestroyed()}
	 */
	public <R, Ex extends Throwable> R invoke(FunctionE<? super char[],R, Ex> function) throws IllegalStateException, Ex {
		char[] copy = getPassword();
		try {
			return function.apply(copy);
		} finally {
			Arrays.fill(copy, (char)0);
		}
	}

	/**
	 * Calls a consumer, providing a copy of the password.
	 * The copy of the password is zeroed once the consumer returns.
	 *
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @throws IllegalStateException when {@link #isDestroyed()}
	 */
	public <Ex extends Throwable> void accept(ConsumerE<? super char[],Ex> consumer) throws IllegalStateException, Ex {
		char[] copy = getPassword();
		try {
			consumer.accept(copy);
		} finally {
			Arrays.fill(copy, (char)0);
		}
	}

	/**
	 * Calls a predicate, providing a copy of the password.
	 * The copy of the password is zeroed once the predicate returns.
	 *
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @throws IllegalStateException when {@link #isDestroyed()}
	 */
	public <Ex extends Throwable> boolean test(PredicateE<? super char[],Ex> predicate) throws IllegalStateException, Ex {
		char[] copy = getPassword();
		try {
			return predicate.test(copy);
		} finally {
			Arrays.fill(copy, (char)0);
		}
	}
}
